package com.shadorc.shadbot.command.game.russianroulette;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.common.util.Snowflake;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

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
                .then(Mono.defer(() -> {
                    if (!player.isAlive()) {
                        return context.reply(Emoji.BROKEN_HEART, context.localize("russianroulette.already.dead")
                                .formatted(FormatUtil.formatDurationWords(context.getLocale(), player.getResetDuration())))
                                .then(Mono.empty());
                    }

                    player.fire();

                    final StringBuilder descBuilder = new StringBuilder(context.localize("russianroulette.pull"));
                    if (player.isAlive()) {
                        final long coins = (long) ThreadLocalRandom.current()
                                .nextInt(Constants.MIN_GAINS, Constants.MAX_GAINS + 1) * player.getRemaining();

                        descBuilder.append(context.localize("russianroulette.win")
                                .formatted(context.localize(coins)));

                        Telemetry.RUSSIAN_ROULETTE_SUMMARY.labels("win").observe(coins);
                        return player.win(coins)
                                .thenReturn(descBuilder);
                    } else {
                        descBuilder.append(context.localize("russianroulette.lose"));

                        Telemetry.RUSSIAN_ROULETTE_SUMMARY.labels("loss").observe(player.getBet());
                        return player.bet()
                                .thenReturn(descBuilder);
                    }
                }))
                .map(StringBuilder::toString)
                .map(description -> ShadbotUtil.getDefaultEmbed(
                        embed -> embed.setAuthor(context.localize("russianroulette.title"),
                                null, context.getAuthorAvatar())
                                .addField(context.localize("russianroulette.tries"),
                                        "%d/6".formatted(player.getRemaining()), false)
                                .setDescription(description)))
                .flatMap(context::reply);
    }

    private RussianRoulettePlayer getPlayer(Snowflake guildId, Snowflake userId) {
        return this.players.computeIfAbsent(Tuples.of(guildId, userId), TupleUtils.function(RussianRoulettePlayer::new));
    }

}
