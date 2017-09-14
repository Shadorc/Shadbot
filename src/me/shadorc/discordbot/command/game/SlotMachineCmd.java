package me.shadorc.discordbot.command.game;

import java.time.temporal.ChronoUnit;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.util.EmbedBuilder;

public class SlotMachineCmd extends AbstractCommand {

	/*
	 * Expected value: 0.125*50 + 0.053*500 + 0.002*12725 - 0.82*10 = 50 coins
	 */

	private enum SlotOptions {
		CHERRIES,
		BELL,
		GIFT
	}

	private static final int PAID_COST = 10;

	private static final int FIRST_GAINS = 50;
	private static final int SECOND_GAINS = 500;
	private static final int THIRD_GAINS = 12725;

	private static final SlotOptions[] SLOTS_ARRAY = new SlotOptions[] {
			SlotOptions.CHERRIES, SlotOptions.CHERRIES, SlotOptions.CHERRIES, SlotOptions.CHERRIES, // Winning chance : 12.5%
			SlotOptions.BELL, SlotOptions.BELL, SlotOptions.BELL, // Winning chance : 5.3%
			SlotOptions.GIFT }; // Winning chance : 0.2%

	private final RateLimiter rateLimiter;

	public SlotMachineCmd() {
		super(Role.USER, "slot_machine", "slot-machine", "slotmachine");
		this.rateLimiter = new RateLimiter(RateLimiter.GAME_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		if(context.getPlayer().getCoins() < PAID_COST) {
			BotUtils.sendMessage(Emoji.BANK + " You don't have enough coins to play the slot machine, one game costs " + PAID_COST + " coins.", context.getChannel());
			return;
		}

		SlotOptions slot1 = SLOTS_ARRAY[MathUtils.rand(SLOTS_ARRAY.length)];
		SlotOptions slot2 = SLOTS_ARRAY[MathUtils.rand(SLOTS_ARRAY.length)];
		SlotOptions slot3 = SLOTS_ARRAY[MathUtils.rand(SLOTS_ARRAY.length)];

		int gains = -PAID_COST;

		if(Utils.allEqual(SlotOptions.CHERRIES, slot1, slot2, slot3)) {
			gains = FIRST_GAINS;
		} else if(Utils.allEqual(SlotOptions.BELL, slot1, slot2, slot3)) {
			gains = SECOND_GAINS;
		} else if(Utils.allEqual(SlotOptions.GIFT, slot1, slot2, slot3)) {
			gains = THIRD_GAINS;
		}
		context.getPlayer().addCoins(gains);

		StringBuilder message = new StringBuilder(
				":" + slot1.toString().toLowerCase() + ": :" + slot2.toString().toLowerCase() + ": :" + slot3.toString().toLowerCase() + ":"
						+ "\nYou " + (gains > 0 ? "win" : "have lost") + " " + Math.abs(gains) + " coins !");
		BotUtils.sendMessage(message.toString(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Play slot machine.**")
				.appendField("Cost", "A game costs " + PAID_COST + " coins.", false)
				.appendField("Gains", "You can win " + FIRST_GAINS + ", " + SECOND_GAINS + " or " + THIRD_GAINS + " coins ! Good luck.", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
