package com.shadorc.shadbot.command.game.rps;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.EnumUtil;
import com.shadorc.shadbot.utils.FormatUtil;
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

        this.addOption("handsign",
                FormatUtil.format(Handsign.values(), Handsign::getHandsign, ", "),
                true,
                ApplicationCommandOptionType.STRING);

        this.players = new ConcurrentHashMap<>();
    }

    @Override
    public Mono<?> execute(Context context) {
        final String handsign = context.getOptionAsString("handsign").orElseThrow();

        final Handsign userHandsign = EnumUtil.parseEnum(Handsign.class, handsign,
                new CommandException(String.format("`%s` is not a valid handsign. %s.",
                        handsign, FormatUtil.options(Handsign.class))));
        final Handsign botHandsign = RandUtil.randValue(Handsign.values());

        final StringBuilder strBuilder = new StringBuilder(String.format("**%s**: %s %s **VS** %s %s :**Shadbot**%n",
                context.getAuthorName(), userHandsign.getHandsign(), userHandsign.getEmoji(),
                botHandsign.getEmoji(), botHandsign.getHandsign()));

        final RpsPlayer player = this.getPlayer(context.getGuildId(), context.getAuthorId());
        if (userHandsign.isSuperior(botHandsign)) {
            final int winStreak = player.getWinStreak().incrementAndGet();
            final long gains = Math.min((long) Constants.GAINS * winStreak, Config.MAX_COINS);
            Telemetry.RPS_SUMMARY.labels("win").observe(gains);
            return player.win(gains)
                    .then(Mono.defer(() -> {
                        strBuilder.append(
                                String.format(Emoji.BANK + " (**%s**) Well done, you win **%s** (Win Streak x%d)!",
                                        context.getAuthorName(), FormatUtil.coins(gains), player.getWinStreak().get()));
                        return context.createFollowupMessage(strBuilder.toString());
                    }));
        } else if (userHandsign == botHandsign) {
            player.getWinStreak().set(0);
            strBuilder.append("It's a draw.");
        } else {
            player.getWinStreak().set(0);
            strBuilder.append("I win !");
        }

        return context.createFollowupMessage(strBuilder.toString());
    }

    private RpsPlayer getPlayer(Snowflake guildId, Snowflake userId) {
        return this.players.computeIfAbsent(Tuples.of(guildId, userId), TupleUtils.function(RpsPlayer::new));
    }

}
