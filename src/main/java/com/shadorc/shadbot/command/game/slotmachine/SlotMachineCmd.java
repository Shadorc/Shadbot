package com.shadorc.shadbot.command.game.slotmachine;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.player.GamblerPlayer;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.StringUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class SlotMachineCmd extends BaseCmd {

    private static final double RAND_FACTOR = 0.25;
    private static final int PAID_COST = 25;

    public SlotMachineCmd() {
        super(CommandCategory.GAME, List.of("slot_machine", "slot-machine", "slotmachine"), "sm");
        this.setGameRateLimiter();
    }

    private static List<SlotOptions> randSlots() {
        // Pseudo-random number between 0 and 100 inclusive
        final int rand = ThreadLocalRandom.current().nextInt(100 + 1);
        if (rand == 0) {
            return List.of(SlotOptions.GIFT, SlotOptions.GIFT, SlotOptions.GIFT);
        }
        if (rand > 0 && rand <= 5) {
            return List.of(SlotOptions.BELL, SlotOptions.BELL, SlotOptions.BELL);
        }
        if (rand > 5 && rand <= 20) {
            return List.of(SlotOptions.CHERRIES, SlotOptions.CHERRIES, SlotOptions.CHERRIES);
        }
        if (rand > 20 && rand <= 50) {
            return List.of(SlotOptions.APPLE, SlotOptions.APPLE, SlotOptions.APPLE);
        }

        final List<SlotOptions> list = new ArrayList<>();
        do {
            final SlotOptions slot = Utils.randValue(SlotOptions.values());
            if (!list.contains(slot)) {
                list.add(slot);
            }
        } while (list.size() != 3);
        return list;
    }

    @Override
    public Mono<Void> execute(Context context) {
        return Utils.requireValidBet(context.getGuildId(), context.getAuthorId(), PAID_COST)
                .map(ignored -> new GamblerPlayer(context.getGuildId(), context.getAuthorId(), PAID_COST))
                .flatMap(player -> player.bet().thenReturn(player))
                .flatMap(player -> {
                    final List<SlotOptions> slots = SlotMachineCmd.randSlots();

                    final StringBuilder strBuilder = new StringBuilder(String.format("%s%n%s (**%s**) ",
                            FormatUtils.format(slots, SlotOptions::getEmoji, " "), Emoji.BANK, context.getUsername()));

                    if (slots.stream().distinct().count() == 1) {
                        final int slotGains = slots.get(0).getGains();
                        final long gains = ThreadLocalRandom.current().nextInt((int) (slotGains * RAND_FACTOR),
                                (int) (slotGains * (RAND_FACTOR + 1)));
                        return player.win(gains)
                                .thenReturn(strBuilder.append(String.format("You win **%s** !", FormatUtils.coins(gains))));
                    } else {
                        return Mono.just(strBuilder.append(String.format("You lose **%s** !", FormatUtils.coins(PAID_COST))));
                    }
                })
                .map(StringBuilder::toString)
                .flatMap(text -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(text, channel)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Play slot machine.")
                .addField("Cost", String.format("A game costs **%s**.", FormatUtils.coins(PAID_COST)), false)
                .addField("Gains", String.format("%s: **%s**, %s: **%s**, %s: **%s**, %s: **%s**." +
                                "%nYou also gain a small random bonus.",
                        StringUtils.capitalizeEnum(SlotOptions.APPLE), FormatUtils.coins(SlotOptions.APPLE.getGains()),
                        StringUtils.capitalizeEnum(SlotOptions.CHERRIES), FormatUtils.coins(SlotOptions.CHERRIES.getGains()),
                        StringUtils.capitalizeEnum(SlotOptions.BELL), FormatUtils.coins(SlotOptions.BELL.getGains()),
                        StringUtils.capitalizeEnum(SlotOptions.GIFT), FormatUtils.coins(SlotOptions.GIFT.getGains())), false)
                .build();
    }

}