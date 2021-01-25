package com.shadorc.shadbot.command.game.russianroulette;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class RussianRouletteCmd extends BaseCmd {

    private final Map<Tuple2<Snowflake, Snowflake>, RussianRoulettePlayer> players;

    public RussianRouletteCmd() {
        super(CommandCategory.GAME, "russian_roulette", "Play russian roulette");
        this.setGameRateLimiter();

        this.players = new ConcurrentHashMap<>();
    }

    @Override
    public Mono<?> execute(Context context) {
        final RussianRoulettePlayer player = this.getPlayer(context.getGuildId(), context.getAuthorId());
        return ShadbotUtil.requireValidBet(context.getGuildId(), context.getAuthorId(), Constants.PAID_COST)
                .filter(__ -> player.isAlive())
                .switchIfEmpty(context.createFollowupMessage(
                        Emoji.BROCKEN_HEART + " (**%s**) Dead people can't play the Russian Roulette... " +
                                "You will be able to play again in %s!",
                        context.getAuthorName(), FormatUtil.formatDurationWords(player.getResetDuration()))
                        .then(Mono.empty()))
                .flatMap(__ -> player.bet())
                .flatMap(__ -> {
                    player.fire();

                    final StringBuilder descBuilder = new StringBuilder("You break a sweat, you pull the trigger...");
                    if (player.isAlive()) {
                        final long coins = (long) ThreadLocalRandom.current()
                                .nextInt(Constants.MIN_GAINS, Constants.MAX_GAINS + 1) * player.getRemaining();

                        descBuilder.append(String.format("\n**\\*click\\*** ... Phew, you are still alive!%nYou get **%s**.",
                                FormatUtil.coins(coins)));

                        Telemetry.RUSSIAN_ROULETTE_SUMMARY.labels("win").observe(coins);
                        return player.cancelBet()
                                .then(player.win(coins))
                                .thenReturn(descBuilder);
                    } else {
                        descBuilder.append("\n**\\*PAN\\*** ... Sorry, you died...");
                        Telemetry.RUSSIAN_ROULETTE_SUMMARY.labels("loss").observe(player.getBet());
                        return Mono.just(descBuilder);
                    }
                })
                .map(StringBuilder::toString)
                .map(description -> ShadbotUtil.getDefaultEmbed(
                        embed -> embed.setAuthor("Russian Roulette", null, context.getAuthorAvatarUrl())
                                .addField("Tries", String.format("%d/6", player.getRemaining()), false)
                                .setDescription(description)))
                .flatMap(context::createFollowupMessage);
    }

    private RussianRoulettePlayer getPlayer(Snowflake guildId, Snowflake userId) {
        return this.players.computeIfAbsent(Tuples.of(guildId, userId), TupleUtils.function(RussianRoulettePlayer::new));
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .addField("Rules",
                        String.format("You initially have 1/6 chance of dying and, the more you win, the more you are likely to lose."
                                + "%nOnce dead, you will not be able to play for %d hours.", Constants.RESET_HOURS), false)
                .addField("Cost", String.format("A game costs **%s**.", FormatUtil.coins(Constants.PAID_COST)), false)
                .addField("Gains", String.format("Each time you win, you randomly get between **%s** and **%s** multiplied"
                                + " by your number of tries.",
                        FormatUtil.coins(Constants.MIN_GAINS), FormatUtil.coins(Constants.MAX_GAINS)), false)
                .build();
    }
}
