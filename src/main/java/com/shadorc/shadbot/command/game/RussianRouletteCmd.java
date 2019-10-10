package com.shadorc.shadbot.command.game;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.player.GamblerPlayer;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class RussianRouletteCmd extends BaseCmd {

    private static final int PAID_COST = 250;

    private static final int MIN_GAINS = 1500;
    private static final int MAX_GAINS = 6000;

    private static final int MIN_LOSE = 4000;
    private static final int MAX_LOSE = 12000;

    public RussianRouletteCmd() {
        super(CommandCategory.GAME, List.of("russian_roulette", "russian-roulette", "russianroulette"), "rr");
        this.setGameRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        Utils.requireValidBet(context.getMember(), Integer.toString(PAID_COST));

        final StringBuilder strBuilder = new StringBuilder(
                String.format(Emoji.DICE + " (**%s**) You break a sweat, you pull the trigger... ", context.getUsername()));

        final GamblerPlayer player = new GamblerPlayer(context.getGuildId(), context.getAuthorId(), PAID_COST);
        player.bet();

        if (ThreadLocalRandom.current().nextInt(6) == 0) {
            final long coins = ThreadLocalRandom.current().nextInt(MIN_LOSE, MAX_LOSE + 1);
            player.lose(coins);
            strBuilder.append(String.format("**PAN** ... Sorry, you died.%nYou lose **%s**.", FormatUtils.coins(coins)));
        } else {
            final long coins = ThreadLocalRandom.current().nextInt(MIN_GAINS, MAX_GAINS + 1);
            player.win(coins);
            strBuilder.append(String.format("**click** ... Phew, you are still alive !%nYou get **%s**.", FormatUtils.coins(coins)));
        }

        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(strBuilder.toString(), channel))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Play Russian roulette.")
                .addField("Cost", String.format("A game costs **%d coins**.", PAID_COST), false)
                .addField("Gains", String.format("You have **1/6 chance** to randomly lose between **%d and %d coins** and **5/6 " +
                        "chances** to randomly get between **%d and %d coins**.", MIN_LOSE, MAX_LOSE, MIN_GAINS, MAX_GAINS), false)
                .build();
    }
}
