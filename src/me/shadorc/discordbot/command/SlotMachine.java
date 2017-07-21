package me.shadorc.discordbot.command;

import me.shadorc.discordbot.Bot;
import me.shadorc.discordbot.utility.Utils;
import sx.blah.discord.handle.obj.IChannel;

public class SlotMachine {

	private enum SlotOptions {
		GRAPES,
		APPLE,
		CHERRIES,
		BELL,
		MELON,
		GIFT
	}

	private static final SlotOptions[] slotsArray = new SlotOptions[] {
			SlotOptions.GRAPES, SlotOptions.GRAPES, SlotOptions.GRAPES, SlotOptions.GRAPES, SlotOptions.GRAPES, SlotOptions.GRAPES,
			SlotOptions.APPLE, SlotOptions.APPLE, SlotOptions.APPLE, SlotOptions.APPLE, SlotOptions.APPLE, SlotOptions.APPLE,
			SlotOptions.CHERRIES, SlotOptions.CHERRIES, SlotOptions.CHERRIES, SlotOptions.CHERRIES, SlotOptions.CHERRIES,
			SlotOptions.BELL, SlotOptions.BELL, SlotOptions.BELL, SlotOptions.BELL,
			SlotOptions.MELON, SlotOptions.MELON, SlotOptions.MELON, 
			SlotOptions.GIFT};

	public static void play(String author, IChannel channel) {
		SlotOptions slot1 = slotsArray[Utils.rand(slotsArray.length)];
		SlotOptions slot2 = slotsArray[Utils.rand(slotsArray.length)];
		SlotOptions slot3 = slotsArray[Utils.rand(slotsArray.length)];

		int gain = -10;

		if(slot1 == SlotOptions.GRAPES && slot2 == SlotOptions.GRAPES && slot3 == SlotOptions.GRAPES) {
			gain = 3;
		} 
		else if(slot1 == SlotOptions.APPLE && slot2 == SlotOptions.APPLE && slot3 == SlotOptions.APPLE) { 
			gain = 6;
		} 
		else if(slot1 == SlotOptions.CHERRIES && slot2 == SlotOptions.CHERRIES && slot3 == SlotOptions.CHERRIES) { 
			gain = 10;
		} 
		else if(slot1 == SlotOptions.BELL && slot2 == SlotOptions.BELL && slot3 == SlotOptions.BELL) { 
			gain = 20;
		} 
		else if(slot1 == SlotOptions.MELON && slot2 == SlotOptions.MELON && slot3 == SlotOptions.MELON) { 
			gain = 40;
		} 
		else if(slot1 == SlotOptions.GIFT && slot2 == SlotOptions.GIFT && slot3 == SlotOptions.GIFT) { 
			gain = 100;
		} 
		Utils.gain(author, gain);

		StringBuilder message = new StringBuilder();
		message.append(":" + slot1.toString().toLowerCase() + ": :" + slot2.toString().toLowerCase() + ": :" + slot3.toString().toLowerCase() + ":");
		message.append("Vous avez "+ (gain > 0 ? "gagné" : "perdu") + gain + " coins !");
		Bot.sendMessage(message.toString(), channel);
	} 
}
