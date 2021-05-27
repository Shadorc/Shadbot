package com.locibot.locibot.command.game.rps;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.DiscordUtil;
import com.locibot.locibot.utils.RandUtil;
import discord4j.common.util.Snowflake;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RpsCmd extends BaseCmd {

    // Key is Guild ID, User ID
    private final Map<Tuple2<Snowflake, Snowflake>, RpsPlayer> players;

    public RpsCmd() {
        super(CommandCategory.GAME, "rps", "Play a Rock–paper–scissors game, win-streak increases gains");
        this.setGameRateLimiter();

        this.addOption(option -> option.name("handsign")
                .description("Your next move")
                .required(true)
                .type(ApplicationCommandOptionType.STRING.getValue())
                .choices(DiscordUtil.toOptions(Handsign.class)));

        this.players = new ConcurrentHashMap<>();
    }

    @Override
    public Mono<?> execute(Context context) {
        final Handsign userHandsign = context.getOptionAsEnum(Handsign.class, "handsign").orElseThrow();
        final Handsign botHandsign = RandUtil.randValue(Handsign.values());

        final StringBuilder strBuilder = new StringBuilder(context.localize("rps.result")
                .formatted(context.getAuthorName(), userHandsign.getHandsign(context), userHandsign.getEmoji(),
                        botHandsign.getEmoji(), botHandsign.getHandsign(context)));

        final RpsPlayer player = this.getPlayer(context.getGuildId(), context.getAuthorId());
        final long gains_min = Math.min((long) Constants.GAINS, Config.MAX_COINS);
        if (userHandsign.isSuperior(botHandsign)) {
            player.incrementWinStream();
            final int winStreak = player.getWinStreak();
            final long gains = Math.min((long) Constants.GAINS * winStreak, Config.MAX_COINS);
            Telemetry.RPS_SUMMARY.labels("win").observe(gains);
            return player.win(gains)
                    .then(Mono.defer(() -> {
                        strBuilder.append(Emoji.BANK + context.localize("rps.win")
                                .formatted(context.getAuthorName(), context.localize(gains),
                                        context.localize(player.getWinStreak())));
                        return context.createFollowupMessage(strBuilder.toString());
                    }));
        } else if (userHandsign == botHandsign) {
            player.resetWinStreak();
            Telemetry.RPS_SUMMARY.labels("draw").observe(gains_min);
            strBuilder.append(context.localize("rps.draw"));
        } else {
            player.resetWinStreak();
            Telemetry.RPS_SUMMARY.labels("lose").observe(gains_min);
            strBuilder.append(context.localize("rps.lose"));
        }

        return context.createFollowupMessage(strBuilder.toString());
    }

    private RpsPlayer getPlayer(Snowflake guildId, Snowflake userId) {
        return this.players.computeIfAbsent(Tuples.of(guildId, userId), TupleUtils.function(RpsPlayer::new));
    }

}
