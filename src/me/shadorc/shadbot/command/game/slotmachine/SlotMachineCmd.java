package me.shadorc.shadbot.command.game.slotmachine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.MoneyEnum;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import reactor.core.publisher.Mono;

public class SlotMachineCmd extends BaseCmd {

	private static final double RAND_FACTOR = 0.25;
	private static final int PAID_COST = 25;

	public SlotMachineCmd() {
		super(CommandCategory.GAME, List.of("slot_machine", "slot-machine", "slotmachine"), "sm");
		this.setGameRateLimiter();
	}

	private List<SlotOptions> randSlots() {
		// Pseudo-random number between 0 and 100 inclusive
		final int rand = ThreadLocalRandom.current().nextInt(100 + 1);
		if(rand == 0) {
			return List.of(SlotOptions.GIFT, SlotOptions.GIFT, SlotOptions.GIFT);
		}
		if(rand > 0 && rand <= 5) {
			return List.of(SlotOptions.BELL, SlotOptions.BELL, SlotOptions.BELL);
		}
		if(rand > 5 && rand <= 20) {
			return List.of(SlotOptions.CHERRIES, SlotOptions.CHERRIES, SlotOptions.CHERRIES);
		}
		if(rand > 20 && rand <= 50) {
			return List.of(SlotOptions.APPLE, SlotOptions.APPLE, SlotOptions.APPLE);
		}

		final List<SlotOptions> list = new ArrayList<>();
		do {
			final SlotOptions slot = Utils.randValue(SlotOptions.values());
			if(!list.contains(slot)) {
				list.add(slot);
			}
		} while(list.size() != 3);
		return list;
	}

	@Override
	public Mono<Void> execute(Context context) {
		Utils.requireValidBet(context.getMember(), Integer.toString(PAID_COST));

		final AtomicInteger gains = new AtomicInteger(-PAID_COST);

		final List<SlotOptions> slots = this.randSlots();
		if(slots.stream().distinct().count() == 1) {
			final int slotGains = slots.get(0).getGains();
			gains.set(ThreadLocalRandom.current().nextInt((int) (slotGains * RAND_FACTOR),
					(int) (slotGains * (RAND_FACTOR + 1))));
		}

		Shadbot.getDatabase().getDBMember(context.getGuildId(), context.getAuthorId()).addCoins(gains.get());
		if(gains.get() > 0) {
			StatsManager.MONEY_STATS.log(MoneyEnum.MONEY_GAINED, this.getName(), gains.get());
		} else {
			StatsManager.MONEY_STATS.log(MoneyEnum.MONEY_LOST, this.getName(), Math.abs(gains.get()));
			Shadbot.getLottery().addToJackpot(Math.abs(gains.get()));
		}

		return context.getChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(String.format("%s%n%s (**%s**) You %s **%s** !",
						FormatUtils.format(slots, SlotOptions::getEmoji, " "), Emoji.BANK, context.getUsername(),
						gains.get() > 0 ? "win" : "lose", FormatUtils.coins(Math.abs(gains.get()))), channel))
				.then();
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Play slot machine.")
				.addField("Cost", String.format("A game costs **%d coins**.", PAID_COST), false)
				.build();
	}

}