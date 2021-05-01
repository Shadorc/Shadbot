package com.shadorc.shadbot.command.game.dice;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.GameCmd;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.NumberUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

public class DiceCmd extends GameCmd<DiceGame> {

    protected static final String JOIN_SUB_COMMAND = "join";
    protected static final String CREATE_SUB_COMMAND = "create";

    public DiceCmd() {
        super("dice", "Start or join a Dice game with a common bet",
                ApplicationCommandOptionType.SUB_COMMAND_GROUP);

        final ApplicationCommandOptionData numberOption = ApplicationCommandOptionData.builder()
                .name("number")
                .description("The number you're betting on")
                .required(true)
                .type(ApplicationCommandOptionType.INTEGER.getValue())
                .build();

        this.addOption(option -> option.name(JOIN_SUB_COMMAND)
                .description("Join a Dice game")
                .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                .addOption(numberOption));
        this.addOption(option -> option.name(CREATE_SUB_COMMAND)
                .description("Start a Dice game")
                .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                .addOption(ApplicationCommandOptionData.builder().name("bet")
                        .description("The common bet")
                        .required(true)
                        .type(ApplicationCommandOptionType.INTEGER.getValue())
                        .build())
                .addOption(numberOption));
    }

    @Override
    public Mono<?> execute(Context context) {
        final long number = context.getOptionAsLong("number").orElseThrow();
        if (!NumberUtil.isBetween(number, 1, 6)) {
            return Mono.error(new CommandException(context.localize("dice.invalid.number")));
        }

        final String subCmd = context.getSubCommandName().orElseThrow();
        if (subCmd.equals(JOIN_SUB_COMMAND)) {
            return this.join(context, (int) number);
        } else if (subCmd.equals(CREATE_SUB_COMMAND)) {
            return this.create(context, (int) number);
        }
        return Mono.error(new IllegalStateException());
    }

    private Mono<?> join(Context context, int number) {
        final DiceGame game = this.getGame(context.getChannelId());
        if (game == null) {
            return Mono.error(new CommandException(context.localize("dice.cannot.join")
                    .formatted(context.getCommandName(), context.getSubCommandGroupName().orElseThrow(), CREATE_SUB_COMMAND)));
        }

        if (game.getPlayers().size() == 6) {
            return Mono.error(new CommandException(context.localize("dice.full")));
        }

        if (game.getPlayers().values().stream().anyMatch(player -> player.getNumber() == number)) {
            return Mono.error(new CommandException(context.localize("dice.number.already.used")));
        }

        return ShadbotUtil.requireValidBet(context.getLocale(), context.getGuildId(), context.getAuthorId(), game.getBet())
                .flatMap(bet -> {
                    final DicePlayer player = new DicePlayer(context.getGuildId(), context.getAuthorId(),
                            context.getAuthorName(), bet, number);
                    if (game.addPlayerIfAbsent(player)) {
                        return player.bet()
                                .then(game.show())
                                .then(context.reply(Emoji.CHECK_MARK, context.localize("dice.joined")));
                    } else {
                        return Mono.error(new CommandException(context.localize("dice.already.participating")));
                    }
                });
    }

    private Mono<?> create(Context context, int number) {
        final long bet = context.getOptionAsLong("bet").orElseThrow();

        if (this.isGameStarted(context.getChannelId())) {
            return Mono.error(new CommandException(context.localize("dice.already.started")
                    .formatted(context.getCommandName(), context.getSubCommandGroupName().orElseThrow(), JOIN_SUB_COMMAND)));
        }

        return ShadbotUtil.requireValidBet(context.getLocale(), context.getGuildId(), context.getAuthorId(), bet)
                .flatMap(__ -> {
                    final DiceGame game = new DiceGame(context, bet);
                    final DicePlayer player = new DicePlayer(context.getGuildId(), context.getAuthorId(),
                            context.getAuthorName(), bet, number);
                    game.addPlayerIfAbsent(player);
                    this.addGame(context.getChannelId(), game);
                    return player.bet()
                            .then(game.start())
                            .then(game.show())
                            .doOnError(err -> game.destroy());
                });
    }

}
