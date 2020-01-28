package com.shadorc.shadbot.command.game.lottery;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.lottery.entity.LotteryGambler;
import com.shadorc.shadbot.db.lottery.entity.LotteryHistoric;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.*;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.http.client.ClientException;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class LotteryCmd extends BaseCmd {

    private static final int PAID_COST = 100;
    public static final int MIN_NUM = 1;
    public static final int MAX_NUM = 100;

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
                    if (dbMember.getCoins() < PAID_COST) {
                        return Mono.error(new CommandException(TextUtils.NOT_ENOUGH_COINS));
                    }

                    final Integer num = NumberUtils.toIntBetweenOrNull(arg, MIN_NUM, MAX_NUM);
                    if (num == null) {
                        return Mono.error(new CommandException(
                                String.format("`%s` is not a valid number, it must be between **%d** and **%d**.",
                                        arg, MIN_NUM, MAX_NUM)));
                    }

                    return dbMember.addCoins(-PAID_COST)
                            .thenReturn(new LotteryGambler(context.getGuildId(), context.getAuthorId(), num))
                            .flatMap(LotteryGambler::insert)
                            .then(context.getChannel())
                            .flatMap(channel -> DiscordUtils.sendMessage(
                                    String.format(Emoji.TICKET + " (**%s**) You bought a lottery ticket and bet " +
                                                    "on number **%d**. Good luck ! The next draw will take place in **%s**.",
                                            context.getUsername(), num, FormatUtils.customDate(LotteryCmd.getDelay())), channel));
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
                .map(tuple -> {
                    final List<LotteryGambler> gamblers = tuple.getT1();
                    final long jackpot = tuple.getT2();
                    final Optional<LotteryHistoric> historic = tuple.getT3();

                    final LotteryEmbedBuilder builder = LotteryEmbedBuilder.create(context)
                            .withGamblers(gamblers)
                            .withJackpot(jackpot);

                    return historic.map(builder::withHistoric)
                            .orElse(builder)
                            .build();
                })
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

    public static Mono<Void> draw(GatewayDiscordClient client) {
        LogUtils.info("Lottery draw started...");
        final int winningNum = ThreadLocalRandom.current().nextInt(MIN_NUM, MAX_NUM + 1);

        return DatabaseManager.getLottery()
                .getGamblers()
                .filter(gambler -> gambler.getNumber() == winningNum)
                .flatMap(winner -> client.getMemberById(winner.getGuildId(), winner.getUserId()))
                .collectList()
                .zipWith(DatabaseManager.getLottery().getJackpot())
                .flatMap(tuple -> {
                    final List<Member> winners = tuple.getT1();
                    final long jackpot = tuple.getT2();

                    LogUtils.info("Lottery draw done (Winning number: %d | %d winner(s) | Prize pool: %d)",
                            winningNum, winners.size(), jackpot);

                    final long coins = Math.min(jackpot / winners.size(), Config.MAX_COINS);

                    return Flux.fromIterable(winners)
                            .flatMap(winner -> DatabaseManager.getGuilds()
                                    .getDBMember(winner.getGuildId(), winner.getId())
                                    .flatMap(dbMember -> dbMember.addCoins(coins))
                                    .then(winner.getPrivateChannel()))
                            .cast(MessageChannel.class)
                            .flatMap(privateChannel -> DiscordUtils.sendMessage(String.format("Congratulations, you " +
                                            "have the winning lottery number! You earn **%s**.",
                                    FormatUtils.coins(coins)), privateChannel))
                            .onErrorResume(ClientException.isStatusCode(HttpResponseStatus.FORBIDDEN.code()), err -> Mono.empty())
                            .then(new LotteryHistoric(jackpot, winners.size(), winningNum).insert())
                            .then(DatabaseManager.getLottery().resetGamblers())
                            .then(Mono.defer(() -> {
                                if (!winners.isEmpty()) {
                                    return DatabaseManager.getLottery().resetJackpot();
                                }
                                return Mono.empty();
                            }));
                })
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Buy a ticket for the lottery or display the current lottery status.")
                .addArg("num", String.format("must be between %d and %d", MIN_NUM, MAX_NUM), true)
                .addField("Info", "One winner is randomly drawn every Sunday at noon (English time)."
                        + "\nIf no one wins, the prize pool is put back into play, "
                        + "if there are multiple winners, the prize pool is splitted between them.", false)
                .addField("Cost", String.format("A ticket costs **%s.**", FormatUtils.coins(PAID_COST)), false)
                .addField("Gains", "The prize pool contains all coins lost at games during the week plus " +
                        "the purchase price of the lottery tickets.", false)
                .build();
    }
}
