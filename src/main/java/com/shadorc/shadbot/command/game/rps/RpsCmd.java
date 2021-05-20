package com.shadorc.shadbot.command.game.rps;

import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.command.GroupCmd;
import com.shadorc.shadbot.core.command.SubCmd;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.RandUtil;
import discord4j.common.util.Snowflake;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RpsCmd extends SubCmd {

    // Key is Guild ID, User ID
    private final Map<Tuple2<Snowflake, Snowflake>, RpsPlayer> players;

    public RpsCmd(final GroupCmd groupCmd) {
        super(groupCmd, CommandCategory.GAME, "rps", "Play a Rock–paper–scissors game, win-streak increases gains");
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
            strBuilder.append(context.localize("rps.draw"));
        } else {
            player.resetWinStreak();
            strBuilder.append(context.localize("rps.lose"));
        }

        return context.createFollowupMessage(strBuilder.toString());
    }

    private RpsPlayer getPlayer(Snowflake guildId, Snowflake userId) {
        return this.players.computeIfAbsent(Tuples.of(guildId, userId), TupleUtils.function(RpsPlayer::new));
    }

}
