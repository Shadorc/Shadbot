package com.shadorc.shadbot.command.game.rps;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
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

public class RpsCmd extends BaseCmd {

    private final Map<Tuple2<Snowflake, Snowflake>, RpsPlayer> players;

    public RpsCmd() {
        super(CommandCategory.GAME, "rps", "Play a Rock–paper–scissors game, win-streak increases gains");
        this.setGameRateLimiter();

        this.addOption("handsign", "Your next move", true, ApplicationCommandOptionType.STRING,
                DiscordUtil.toOptions(Handsign.class));

        this.players = new ConcurrentHashMap<>();
    }

    @Override
    public Mono<?> execute(Context context) {
        final Handsign userHandsign = context.getOptionAsEnum(Handsign.class, "handsign").orElseThrow();
        final Handsign botHandsign = RandUtil.randValue(Handsign.values());

        final StringBuilder strBuilder = new StringBuilder(context.localize("rps.result")
                .formatted(context.getAuthorName(), userHandsign.getHandsign(context.getI18nContext()), userHandsign.getEmoji(),
                        botHandsign.getEmoji(), botHandsign.getHandsign(context.getI18nContext())));

        final RpsPlayer player = this.getPlayer(context.getGuildId(), context.getAuthorId());
        if (userHandsign.isSuperior(botHandsign)) {
            final int winStreak = player.getWinStreak().incrementAndGet();
            final long gains = Math.min((long) Constants.GAINS * winStreak, Config.MAX_COINS);
            Telemetry.RPS_SUMMARY.labels("win").observe(gains);
            return player.win(gains)
                    .then(Mono.defer(() -> {
                        strBuilder.append(Emoji.BANK + context.localize("rps.win")
                                .formatted(context.getAuthorName(), context.localize(gains),
                                        context.localize(player.getWinStreak().get())));
                        return context.reply(strBuilder.toString());
                    }));
        } else if (userHandsign == botHandsign) {
            player.getWinStreak().set(0);
            strBuilder.append(context.localize("rps.draw"));
        } else {
            player.getWinStreak().set(0);
            strBuilder.append(context.localize("rps.lose"));
        }

        return context.reply(strBuilder.toString());
    }

    private RpsPlayer getPlayer(Snowflake guildId, Snowflake userId) {
        return this.players.computeIfAbsent(Tuples.of(guildId, userId), TupleUtils.function(RpsPlayer::new));
    }

}
