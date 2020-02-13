package com.shadorc.shadbot.command.game.dice;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.GameCmd;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.exception.MissingArgumentException;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class DiceCmd extends GameCmd<DiceGame> {

    protected static final float MULTIPLIER = 4.5f;

    public DiceCmd() {
        super(List.of("dice"));
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(1, 2);

        // This boolean indicates if the user is trying to join or to create a game
        final boolean isJoining = args.size() == 1;

        final Integer number = NumberUtils.toIntBetweenOrNull(args.get(0), 1, 6);
        if (number == null) {
            return Mono.error(new CommandException(
                    String.format("`%s` is not a valid number, must be between **1** and **6**.",
                            args.get(0))));
        }

        // A game is already started...
        if (this.getManagers().containsKey(context.getChannelId())) {
            // ... and the user is trying to create a game
            if (!isJoining) {
                return context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.INFO
                                        + " (**%s**) A **Dice Game** has already been started. "
                                        + "Use `%s%s <num>` to join it.",
                                context.getUsername(), context.getPrefix(), this.getName()), channel))
                        .then();
            }

            final DiceGame diceManager = this.getManagers().get(context.getChannelId());

            if (diceManager.getPlayers().containsKey(context.getAuthorId())) {
                return context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.INFO
                                        + " (**%s**) You're already participating.",
                                context.getUsername()), channel))
                        .then();
            }

            if (diceManager.getPlayers().size() == 6) {
                return context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.GREY_EXCLAMATION
                                        + " (**%s**) Sorry, there are already 6 players.",
                                context.getUsername()), channel))
                        .then();
            }

            if (diceManager.getPlayers().values().stream().anyMatch(player -> player.getNumber() == number)) {
                return context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.GREY_EXCLAMATION
                                        + " (**%s**) This number has already been bet, please try with another one.",
                                context.getUsername()), channel))
                        .then();
            }

            return Utils.requireValidBet(context.getGuildId(), context.getAuthorId(), diceManager.getBet())
                    .flatMap(bet -> {
                        final DicePlayer player = new DicePlayer(context.getGuildId(), context.getAuthorId(), bet, number);
                        if (diceManager.addPlayerIfAbsent(player)) {
                            return player.bet().then(diceManager.show());
                        } else {
                            return diceManager.show();
                        }
                    });
        }
        // A game is not already started...
        else {
            // ... and the user tries to join a game
            if (isJoining) {
                return Mono.error(new MissingArgumentException());
            }

            return Utils.requireValidBet(context.getGuildId(), context.getAuthorId(), args.get(1))
                    .map(bet -> this.getManagers().computeIfAbsent(context.getChannelId(),
                            ignored -> new DiceGame(this, context, bet)))
                    .flatMap(diceManager -> {
                        final DicePlayer player = new DicePlayer(context.getGuildId(), context.getAuthorId(),
                                diceManager.getBet(), number);
                        if (diceManager.addPlayerIfAbsent(player)) {
                            return player.bet()
                                    .then(diceManager.start())
                                    .then(diceManager.show());
                        } else {
                            return diceManager.start()
                                    .then(diceManager.show());
                        }
                    });
        }
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Start a dice game with a common bet.")
                .addArg("num", "number between 1 and 6\nYou can't bet on a number that has already " +
                        "been chosen by another player.", false)
                .addArg("bet", false)
                .addField("Gains", String.format("The winner gets the prize pool plus **%.1f times** " +
                        "his bet.", MULTIPLIER), false)
                .build();
    }
}
