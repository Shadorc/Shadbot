/*
package com.shadorc.shadbot.command.game.roulette;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.GameCmd;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.EnumUtil;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.regex.Pattern;

public class RouletteCmd extends GameCmd<RouletteGame> {

    // Match [1-36]
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^([1-9]|1[0-9]|2[0-9]|3[0-6])$");

    public enum Place {
        RED, BLACK, ODD, EVEN, LOW, HIGH
    }

    public RouletteCmd() {
        super("roulette", "Play roulette");
        this.addOption(option -> option.name("bet")
                .description("Your bet")
                .required(true)
                .type(ApplicationCommandOptionType.INTEGER.getValue()));
        this.addOption(option -> option.name("place")
                .description("number between 1 and 36, %s"
                        .formatted(FormatUtil.format(Place.class, it -> it.name().toLowerCase(), ", ")))
                .required(true)
                .type(ApplicationCommandOptionType.STRING.getValue()));
    }

    @Override
    public Mono<?> execute(Context context) {
        final long bet = context.getOptionAsLong("bet").orElseThrow();
        final String place = context.getOptionAsString("place").orElseThrow().toLowerCase();

        return ShadbotUtil.requireValidBet(context.getLocale(), context.getGuildId(), context.getAuthorId(), bet)
                .flatMap(__ -> {
                    // Match [1-36], red, black, odd, even, high or low
                    if (!NUMBER_PATTERN.matcher(place).matches() && EnumUtil.parseEnum(Place.class, place) == null) {
                        return Mono.error(new CommandException(context.localize("roulette.invalid.place")
                                .formatted(place, FormatUtil.format(Place.class, it -> it.name().toLowerCase(), ", "))));
                    }

                    if (this.getManagers().containsKey(context.getChannelId())) {
                        return Mono.just(this.getManagers().get(context.getChannelId()));
                    } else {
                        final RouletteGame game = new RouletteGame(this, context);
                        this.getManagers().put(context.getChannelId(), game);
                        return game.start()
                                .thenReturn(game);
                    }
                })
                .flatMap(rouletteGame -> {
                    final RoulettePlayer player = new RoulettePlayer(context.getGuildId(), context.getAuthorId(), bet, place);
                    if (rouletteGame.addPlayerIfAbsent(player)) {
                        return player.bet().then(rouletteGame.show());
                    } else {
                        return context.reply(Emoji.INFO, context.localize("roulette.already.participating"));
                    }
                });
    }
}
*/
