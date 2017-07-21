package me.shadorc.discordbot.command;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Utils;
import sx.blah.discord.handle.obj.IChannel;

public class SlotMachineCommand extends Command {
	
	private enum SlotOptions {
		CHERRIES,
		BELL,
		GIFT
	}
	
	private final SlotOptions[] slotsArray = new SlotOptions[] {
			SlotOptions.CHERRIES, SlotOptions.CHERRIES, SlotOptions.CHERRIES, SlotOptions.CHERRIES,
			SlotOptions.BELL, SlotOptions.BELL, SlotOptions.BELL,
			SlotOptions.GIFT};

	public SlotMachineCommand() {
		super("machine_sous");
	}

	@Override
	public void execute(Context context) {
		this.play(context.getMessage().getAuthor().getName(), context.getChannel());		
	}
	
	private void play(String author, IChannel channel) {
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
		Utils.gain(author, gain);

		StringBuilder message = new StringBuilder();
		message.append(":" + slot1.toString().toLowerCase() + ": :" + slot2.toString().toLowerCase() + ": :" + slot3.toString().toLowerCase() + ":");
		message.append("\nVous avez "+ (gain > 0 ? "gagné" : "perdu") + " " + Math.abs(gain) + " coins !");
		BotUtils.sendMessage(message.toString(), channel);
	}
}
