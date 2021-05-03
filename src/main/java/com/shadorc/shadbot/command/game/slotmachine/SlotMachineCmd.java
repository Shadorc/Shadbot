package com.shadorc.shadbot.command.game.slotmachine;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.player.GamblerPlayer;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.RandUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SlotMachineCmd extends BaseCmd {

    public SlotMachineCmd() {
        super(CommandCategory.GAME, "slot_machine", "Play slot machine");
        this.setGameRateLimiter();
    }

    @Override
    public Mono<?> execute(Context context) {
        final GamblerPlayer player = new GamblerPlayer(context.getGuildId(), context.getAuthorId(), Constants.PAID_COST);

        return ShadbotUtil.requireValidBet(context.getLocale(), context.getGuildId(), context.getAuthorId(), Constants.PAID_COST)
                .then(player.bet())
                .then(Mono.defer(() -> {
                    final List<SlotOptions> slots = SlotMachineCmd.randSlots();

                    final StringBuilder strBuilder = new StringBuilder()
                            .append(FormatUtil.format(slots, slot -> slot.getEmoji().toString(), " "))
                            .append('\n')
                            .append(Emoji.BANK)
                            .append(' ');

                    if (slots.stream().distinct().count() == 1) {
                        final int slotGains = slots.get(0).getGains();
                        final long gains = ThreadLocalRandom.current().nextInt(
                                (int) (slotGains * Constants.RAND_FACTOR),
                                (int) (slotGains * (Constants.RAND_FACTOR + 1)));
                        strBuilder.append(context.localize("slotmachine.win").formatted(context.localize(gains)));
                        Telemetry.SLOT_MACHINE_SUMMARY.labels("win").observe(gains);
                        return player.win(gains)
                                .thenReturn(strBuilder);
                    } else {
                        strBuilder.append(context.localize("slotmachine.lose")
                                .formatted(context.localize(Constants.PAID_COST)));
                        Telemetry.SLOT_MACHINE_SUMMARY.labels("loss").observe(Constants.PAID_COST);
                        return Mono.just(strBuilder);
                    }
                }))
                .map(StringBuilder::toString)
                .flatMap(context::createFollowupMessage);
    }

    private static List<SlotOptions> randSlots() {
        // Pseudo-random number between 0 and 100 inclusive
        final int rand = ThreadLocalRandom.current().nextInt(100 + 1);
        if (rand == 0) {
            return List.of(SlotOptions.GIFT, SlotOptions.GIFT, SlotOptions.GIFT);
        }
        if (rand <= 5) {
            return List.of(SlotOptions.BELL, SlotOptions.BELL, SlotOptions.BELL);
        }
        if (rand <= 20) {
            return List.of(SlotOptions.CHERRIES, SlotOptions.CHERRIES, SlotOptions.CHERRIES);
        }
        if (rand <= 50) {
            return List.of(SlotOptions.APPLE, SlotOptions.APPLE, SlotOptions.APPLE);
        }

        final List<SlotOptions> list = new ArrayList<>();
        do {
            final SlotOptions slot = RandUtil.randValue(SlotOptions.values());
            if (!list.contains(slot)) {
                list.add(slot);
            }
        } while (list.size() != 3);

        return Collections.unmodifiableList(list);
    }

}
