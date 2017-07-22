package me.shadorc.discordbot.command;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Utils;

public class SlotMachineCmd extends Command {

	private enum SlotOptions {
		CHERRIES,
		BELL,
		GIFT
	}

	/*
	 * CHERRIES : 4/8
	 * BELL : 3/8
	 * GIFT 1/8
	 *
	 * 50€ : 12.5%
	 * 100€ : 5.3%
	 * 5000€ : 0.2%
	 */

	private final SlotOptions[] slotsArray = new SlotOptions[] {
			SlotOptions.CHERRIES, SlotOptions.CHERRIES, SlotOptions.CHERRIES, SlotOptions.CHERRIES,
			SlotOptions.BELL, SlotOptions.BELL, SlotOptions.BELL,
			SlotOptions.GIFT};

	public SlotMachineCmd() {
		super("machine_sous");
	}

	@Override
	public void execute(Context context) {
		if(Storage.get(context.getAuthor().getName()) < 5) {
			BotUtils.sendMessage("Vous ne pouvez pas jouer aux machines à sous, vous n'avez plus assez de coins !", context.getChannel());
			return;
		}

		SlotOptions slot1 = slotsArray[Utils.rand(slotsArray.length)];
		SlotOptions slot2 = slotsArray[Utils.rand(slotsArray.length)];
		SlotOptions slot3 = slotsArray[Utils.rand(slotsArray.length)];

		int gain = -5;

		if(slot1 == SlotOptions.CHERRIES && slot2 == SlotOptions.CHERRIES && slot3 == SlotOptions.CHERRIES) {
			gain = 50;
		}
		else if(slot1 == SlotOptions.BELL && slot2 == SlotOptions.BELL && slot3 == SlotOptions.BELL) {
			gain = 100;
		}
		else if(slot1 == SlotOptions.GIFT && slot2 == SlotOptions.GIFT && slot3 == SlotOptions.GIFT) {
			gain = 5000;
		}
		Utils.gain(context.getAuthor().getName(), gain);

		StringBuilder message = new StringBuilder();
		message.append(":" + slot1.toString().toLowerCase() + ": :" + slot2.toString().toLowerCase() + ": :" + slot3.toString().toLowerCase() + ":");
		message.append("\nVous avez "+ (gain > 0 ? "gagné" : "perdu") + " " + Math.abs(gain) + " coins !");
		BotUtils.sendMessage(message.toString(), context.getChannel());
	}
}
