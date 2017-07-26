package me.shadorc.discordbot.command.game;

import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Utils;

public class SlotMachineCmd extends Command {

	private enum SlotOptions {
		CHERRIES,
		BELL,
		GIFT
	}

	private static final int PAID_COST = 5;
	private static final SlotOptions[] slotsArray = new SlotOptions[] {
			SlotOptions.CHERRIES, SlotOptions.CHERRIES, SlotOptions.CHERRIES, SlotOptions.CHERRIES, //Winning chance : 12.5%
			SlotOptions.BELL, SlotOptions.BELL, SlotOptions.BELL, 									//Winning chance : 5.3%
			SlotOptions.GIFT}; 																		//Winning chance : 0.2%

	public SlotMachineCmd() {
		super(false, "machine_sous", "slot_machine");
	}

	@Override
	public void execute(Context context) {
		if(Storage.getCoins(context.getGuild(), context.getAuthor()) < PAID_COST) {
			BotUtils.sendMessage(":slot_machine: Vous n'avez pas assez de coins pour jouer aux machines à sous, il vous en faut minimum " + PAID_COST + " !", context.getChannel());
			return;
		}

		SlotOptions slot1 = slotsArray[Utils.rand(slotsArray.length)];
		SlotOptions slot2 = slotsArray[Utils.rand(slotsArray.length)];
		SlotOptions slot3 = slotsArray[Utils.rand(slotsArray.length)];

		int gain = -PAID_COST;

		if(slot1 == SlotOptions.CHERRIES && slot2 == SlotOptions.CHERRIES && slot3 == SlotOptions.CHERRIES) {
			gain = 30;
		}
		else if(slot1 == SlotOptions.BELL && slot2 == SlotOptions.BELL && slot3 == SlotOptions.BELL) {
			gain = 150;
		}
		else if(slot1 == SlotOptions.GIFT && slot2 == SlotOptions.GIFT && slot3 == SlotOptions.GIFT) {
			gain = 5000;
		}
		Utils.gain(context.getGuild(), context.getAuthor(), gain);

		StringBuilder message = new StringBuilder();
		message.append(":" + slot1.toString().toLowerCase() + ": :" + slot2.toString().toLowerCase() + ": :" + slot3.toString().toLowerCase() + ":");
		message.append("\nVous avez "+ (gain > 0 ? "gagné" : "perdu") + " " + Math.abs(gain) + " coins !");
		BotUtils.sendMessage(message.toString(), context.getChannel());
	}
}
