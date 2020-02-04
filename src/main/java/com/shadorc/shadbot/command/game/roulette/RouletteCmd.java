package com.shadorc.shadbot.command.game.roulette;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.GameCmd;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class RouletteCmd extends GameCmd<RouletteGame> {

    // Match [1-36]
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^([1-9]|1[0-9]|2[0-9]|3[0-6])$");

    public enum Place {
        RED, BLACK, ODD, EVEN, LOW, HIGH;
    }

    public RouletteCmd() {
        super(List.of("roulette"));
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(2);

        final String place = args.get(1).toLowerCase();
        return Utils.requireValidBet(context.getGuildId(), context.getAuthorId(), args.get(0))
                .flatMap(bet -> {
                    // Match [1-36], red, black, odd, even, high or low
                    if (!NUMBER_PATTERN.matcher(place).matches() && Utils.parseEnum(Place.class, place) == null) {
                        return Mono.error(new CommandException(
                                String.format("`%s` is not a valid place, must be a number between **1 and 36**, %s.",
                                place, FormatUtils.format(Place.values(),
                                                value -> String.format("**%s**", value.toString().toLowerCase()), ", "))));
                    }

                    if (this.getManagers().containsKey(context.getChannelId())) {
                        return Mono.just(Tuples.of(this.getManagers().get(context.getChannelId()), bet));
                    } else {
                        final RouletteGame game = new RouletteGame(this, context);
                        this.getManagers().put(context.getChannelId(), game);
                        return game.start()
                                .thenReturn(Tuples.of(game, bet));
                    }
                })
                .flatMap(tuple -> {
                    final RouletteGame rouletteGame = tuple.getT1();
                    final long bet = tuple.getT2();

                    final RoulettePlayer player = new RoulettePlayer(context.getGuildId(), context.getAuthorId(), bet, place);
                    if (rouletteGame.addPlayerIfAbsent(player)) {
                        return player.bet().then(rouletteGame.show());
                    } else {
                        return context.getChannel()
                                .flatMap(channel -> DiscordUtils.sendMessage(
                                        String.format(Emoji.INFO + " (**%s**) You're already participating.",
                                                context.getUsername()), channel))
                                .then();
                    }
                });
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Play a roulette game in which everyone can participate.")
                .addArg("bet", false)
                .addArg("place", String.format("number between 1 and 36, %s",
                        FormatUtils.format(Place.class, ", ")), false)
                .addField("Info", "**low** - numbers between 1 and 18"
                        + "\n**high** - numbers between 19 and 36", false)
                .addField("Gains", "The game follows the same rules and winnings as real Roulette.", false)
                .build();
    }
}
