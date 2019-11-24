package com.shadorc.shadbot.command.game;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBMember;
import com.shadorc.shadbot.db.lottery.LotteryCollection;
import com.shadorc.shadbot.db.lottery.entity.LotteryGambler;
import com.shadorc.shadbot.db.lottery.entity.LotteryHistoric;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.*;
import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.http.client.ClientException;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LotteryCmd extends BaseCmd {

    private static final int PAID_COST = 100;
    private static final int MIN_NUM = 1;
    private static final int MAX_NUM = 100;

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

        final DBMember dbMember = DatabaseManager.getGuilds().getDBMember(context.getGuildId(), context.getAuthorId());
        if (dbMember.getCoins() < PAID_COST) {
            return Mono.error(new CommandException(TextUtils.NOT_ENOUGH_COINS));
        }

        if (DatabaseManager.getLottery().isGambler(context.getAuthorId())) {
            return Mono.error(new CommandException("You're already participating."));
        }

        final Integer num = NumberUtils.toIntBetweenOrNull(arg, MIN_NUM, MAX_NUM);
        if (num == null) {
            return Mono.error(new CommandException(String.format("`%s` is not a valid number, it must be between **%d** and **%d**.",
                    arg, MIN_NUM, MAX_NUM)));
        }

        dbMember.addCoins(-PAID_COST);

        new LotteryGambler(context.getGuildId(), context.getAuthorId(), num)
                .insert();

        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.TICKET + " (**%s**) You bought a lottery ticket and bet on number **%d**. Good luck ! "
                                + "The next draw will take place in **%s**.",
                        context.getUsername(), num, FormatUtils.customDate(LotteryCmd.getDelay())), channel))
                .then();
    }

    private Mono<Message> show(Context context) {
        final List<LotteryGambler> gamblers = DatabaseManager.getLottery().getGamblers();

        final Consumer<EmbedCreateSpec> embedConsumer = DiscordUtils.getDefaultEmbed()
                .andThen(embed -> {
                    embed.setAuthor("Lottery", null, context.getAvatarUrl())
                            .setThumbnail("https://i.imgur.com/peLGtkS.png")
                            .setDescription(String.format("The next draw will take place in **%s**%nTo participate, type: `%s%s %d-%d`",
                                    FormatUtils.customDate(LotteryCmd.getDelay()),
                                    context.getPrefix(), this.getName(), MIN_NUM, MAX_NUM))
                            .addField("Number of participants", Integer.toString(gamblers.size()), false)
                            .addField("Prize pool", FormatUtils.coins(DatabaseManager.getLottery().getJackpot()), false);

                    gamblers.stream()
                            .filter(lotteryGambler -> lotteryGambler.getUserId().equals(context.getAuthorId()))
                            .findAny()
                            .ifPresent(gambler -> embed.setFooter(String.format("You bet on number %d.", gambler.getNumber()),
                                    "https://i.imgur.com/btJAaAt.png"));

                    DatabaseManager.getLottery().getHistoric().ifPresent(historic -> {
                        final String people;
                        switch (historic.getWinnerCount()) {
                            case 0:
                                people = "nobody";
                                break;
                            case 1:
                                people = "one person";
                                break;
                            default:
                                people = historic.getWinnerCount() + " people";
                                break;
                        }

                        embed.addField("Historic",
                                String.format("Last week, the prize pool contained **%s**, the winning number was **%d** and **%s won**.",
                                        FormatUtils.coins(historic.getJackpot()), historic.getNumber(), people),
                                false);
                    });
                });

        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(embedConsumer, channel));
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

    public static Mono<Void> draw(DiscordClient client) {
        LogUtils.info("Lottery draw started...");
        final int winningNum = ThreadLocalRandom.current().nextInt(MIN_NUM, MAX_NUM + 1);

        final List<LotteryGambler> winners = DatabaseManager.getLottery().getGamblers().stream()
                .filter(gambler -> gambler.getNumber() == winningNum)
                .collect(Collectors.toList());

        LogUtils.info("Lottery draw done (Winning number: %d | %d winner(s) | Prize pool: %d)",
                winningNum, winners.size(), DatabaseManager.getLottery().getJackpot());

        final long jackpot = DatabaseManager.getLottery().getJackpot();
        return Flux.fromIterable(winners)
                .flatMap(winner -> client.getMemberById(winner.getGuildId(), winner.getUserId()))
                .flatMap(member -> {
                    final long coins = Math.min(jackpot / winners.size(), Config.MAX_COINS);
                    DatabaseManager.getGuilds().getDBMember(member.getGuildId(), member.getId()).addCoins(coins);
                    return member.getPrivateChannel()
                            .cast(MessageChannel.class)
                            .flatMap(privateChannel -> DiscordUtils.sendMessage(String.format("Congratulations, you have the winning lottery number! You earn **%s**.",
                                    FormatUtils.coins(coins)), privateChannel))
                            .onErrorResume(ClientException.isStatusCode(HttpResponseStatus.FORBIDDEN.code()), err -> Mono.empty());
                })
                .then(Mono.fromRunnable(() -> {
                    final LotteryCollection lottery = DatabaseManager.getLottery();

                    new LotteryHistoric(jackpot, winners.size(), winningNum).insert();
                    lottery.resetGamblers();
                    if (!winners.isEmpty()) {
                        lottery.resetJackpot();
                    }
                }));
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Buy a ticket for the lottery or display the current lottery status.")
                .addArg("num", String.format("must be between %d and %d", MIN_NUM, MAX_NUM), true)
                .addField("Info", "One winner is randomly drawn every Sunday at noon (English time)."
                        + "\nIf no one wins, the prize pool is put back into play, "
                        + "if there are multiple winners, the prize pool is splitted between them.", false)
                .addField("Cost", String.format("A ticket costs **%d coins.**", PAID_COST), false)
                .addField("Gains", "The prize pool contains all coins lost at games during the week plus the purchase price of the lottery tickets.", false)
                .build();
    }
}
