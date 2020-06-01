package com.shadorc.shadbot.command.game.lottery;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.lottery.entity.LotteryGambler;
import com.shadorc.shadbot.db.lottery.entity.LotteryHistoric;
import com.shadorc.shadbot.db.users.entity.achievement.Achievement;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.utils.*;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.http.client.ClientException;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class LotteryCmd extends BaseCmd {

    public LotteryCmd() {
        super(CommandCategory.GAME, List.of("lottery", "lotto"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        if (context.getArg().isEmpty()) {
            return this.show(context).then();
        }

        final String arg = context.requireArg();

        return DatabaseManager.getGuilds()
                .getDBMember(context.getGuildId(), context.getAuthorId())
                .filterWhen(dbMember -> DatabaseManager.getLottery().isGambler(dbMember.getId())
                        .map(isParticipating -> !isParticipating))
                .switchIfEmpty(Mono.error(new CommandException("You're already participating.")))
                .flatMap(dbMember -> {
                    if (dbMember.getCoins() < Constants.PAID_COST) {
                        return Mono.error(new CommandException(ShadbotUtils.NOT_ENOUGH_COINS));
                    }

                    final Integer num = NumberUtils.toIntBetweenOrNull(arg, Constants.MIN_NUM, Constants.MAX_NUM);
                    if (num == null) {
                        return Mono.error(new CommandException(
                                String.format("`%s` is not a valid number, it must be between **%d** and **%d**.",
                                        arg, Constants.MIN_NUM, Constants.MAX_NUM)));
                    }

                    return dbMember.addCoins(-Constants.PAID_COST)
                            .thenReturn(new LotteryGambler(context.getGuildId(), context.getAuthorId(), num))
                            .flatMap(LotteryGambler::insert)
                            .then(context.getChannel())
                            .flatMap(channel -> DiscordUtils.sendMessage(
                                    String.format(Emoji.TICKET + " (**%s**) You bought a lottery ticket and bet " +
                                                    "on number **%d**. Good luck ! The next draw will take place in **%s**.",
                                            context.getUsername(), num, FormatUtils.formatDurationWords(LotteryCmd.getDelay())), channel));
                })
                .then();
    }

    private Mono<Message> show(Context context) {
        final Mono<List<LotteryGambler>> getGamblers = DatabaseManager.getLottery()
                .getGamblers()
                .collectList();

        final Mono<Long> getJackpot = DatabaseManager.getLottery()
                .getJackpot();

        final Mono<Optional<LotteryHistoric>> getHistoric = DatabaseManager.getLottery()
                .getHistoric()
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty());

        return Mono.zip(getGamblers, getJackpot, getHistoric)
                .map(TupleUtils.function((gamblers, jackpot, historic) -> {
                    final LotteryEmbedBuilder builder = LotteryEmbedBuilder.create(context)
                            .withGamblers(gamblers)
                            .withJackpot(jackpot);

                    return historic.map(builder::withHistoric)
                            .orElse(builder)
                            .build();
                }))
                .flatMap(embed -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(embed, channel)));
    }

    public static Duration getDelay() {
        ZonedDateTime nextDate = ZonedDateTime.now()
                .with(DayOfWeek.SUNDAY)
                .withHour(12)
                .withMinute(0)
                .withSecond(0);
        if (nextDate.isBefore(ZonedDateTime.now())) {
            nextDate = nextDate.plusWeeks(1);
        }

        return Duration.ofMillis(TimeUtils.getMillisUntil(nextDate.toInstant()));
    }

    public static Mono<Void> draw(GatewayDiscordClient gateway) {
        DEFAULT_LOGGER.info("Lottery draw started...");
        final int winningNum = ThreadLocalRandom.current().nextInt(Constants.MIN_NUM, Constants.MAX_NUM + 1);

        return DatabaseManager.getLottery()
                .getGamblers()
                .filter(gambler -> gambler.getNumber() == winningNum)
                .flatMap(winner -> gateway.getMemberById(winner.getGuildId(), winner.getUserId()))
                .collectList()
                .zipWith(DatabaseManager.getLottery().getJackpot())
                .flatMap(TupleUtils.function((winners, jackpot) -> {
                    DEFAULT_LOGGER.info("Lottery draw done (Winning number: {} | {} winner(s) | Prize pool: {})",
                            winningNum, winners.size(), jackpot);

                    final Long coins = winners.isEmpty() ? null : Math.min(jackpot / winners.size(), Config.MAX_COINS);

                    return Flux.fromIterable(winners)
                            .flatMap(member -> DatabaseManager.getGuilds()
                                    .getDBMember(member.getGuildId(), member.getId())
                                    .flatMap(dbMember -> dbMember.addCoins(coins))
                                    .and(DatabaseManager.getUsers()
                                            .getDBUser(member.getId())
                                            .flatMap(dbUser -> dbUser.unlockAchievement(Achievement.BINGO)))
                                    .then(member.getPrivateChannel()))
                            .cast(MessageChannel.class)
                            .flatMap(privateChannel -> DiscordUtils.sendMessage(String.format("Congratulations, you " +
                                            "have the winning lottery number! You earn **%s**.",
                                    FormatUtils.coins(coins)), privateChannel))
                            .onErrorResume(ClientException.isStatusCode(HttpResponseStatus.FORBIDDEN.code()), err -> Mono.empty())
                            .then(new LotteryHistoric(jackpot, winners.size(), winningNum).insert())
                            .then(DatabaseManager.getLottery().resetGamblers())
                            .then(Mono.defer(() -> {
                                if (winners.isEmpty()) {
                                    return Mono.empty();
                                }
                                return DatabaseManager.getLottery().resetJackpot();
                            }));
                }))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Buy a ticket for the lottery or display the current lottery status.")
                .addArg("num", String.format("must be between %d and %d", Constants.MIN_NUM, Constants.MAX_NUM), true)
                .addField("Info", "One winner is randomly drawn every Sunday at noon (English time)."
                        + "\nIf no one wins, the prize pool is put back into play, "
                        + "if there are multiple winners, the prize pool is splitted between them.", false)
                .addField("Cost", String.format("A ticket costs **%s.**", FormatUtils.coins(Constants.PAID_COST)), false)
                .addField("Gains", "The prize pool contains all coins lost at games during the week plus " +
                        "the purchase price of the lottery tickets.", false)
                .build();
    }
}
