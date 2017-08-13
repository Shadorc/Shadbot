package me.shadorc.discordbot.command.game;

import java.time.temporal.ChronoUnit;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.MathUtils;
import sx.blah.discord.util.EmbedBuilder;

public class SlotMachineCmd extends Command {

	private enum SlotOptions {
		CHERRIES,
		BELL,
		GIFT
	}

	private static final int PAID_COST = 10;
	private static final SlotOptions[] slotsArray = new SlotOptions[] {
			SlotOptions.CHERRIES, SlotOptions.CHERRIES, SlotOptions.CHERRIES, SlotOptions.CHERRIES, // Winning chance : 12.5%
			SlotOptions.BELL, SlotOptions.BELL, SlotOptions.BELL, // Winning chance : 5.3%
			SlotOptions.GIFT }; // Winning chance : 0.2%

	private final RateLimiter rateLimiter;

	public SlotMachineCmd() {
		super(false, "slot_machine", "machine_sous");
		this.rateLimiter = new RateLimiter(10, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isLimitedAndNotWarned(context.getGuild(), context.getAuthor())) {
			rateLimiter.warn("You can use the slot machine only once every " + rateLimiter.getTimeout() + " seconds.", context);
			return;
		}

		if(context.getUser().getCoins() < PAID_COST) {
			BotUtils.sendMessage(Emoji.BANK + " You don't have enough coins to play the slot machine, one game costs " + PAID_COST + " coins.", context.getChannel());
			return;
		}

		SlotOptions slot1 = slotsArray[MathUtils.rand(slotsArray.length)];
		SlotOptions slot2 = slotsArray[MathUtils.rand(slotsArray.length)];
		SlotOptions slot3 = slotsArray[MathUtils.rand(slotsArray.length)];

		int gains = -PAID_COST;

		if(slot1 == SlotOptions.CHERRIES && slot2 == SlotOptions.CHERRIES && slot3 == SlotOptions.CHERRIES) {
			gains = 30;
		} else if(slot1 == SlotOptions.BELL && slot2 == SlotOptions.BELL && slot3 == SlotOptions.BELL) {
			gains = 150;
		} else if(slot1 == SlotOptions.GIFT && slot2 == SlotOptions.GIFT && slot3 == SlotOptions.GIFT) {
			gains = 3000;
		}
		context.getUser().addCoins(gains);

		StringBuilder message = new StringBuilder();
		message.append(":" + slot1.toString().toLowerCase() + ": :" + slot2.toString().toLowerCase() + ": :" + slot3.toString().toLowerCase() + ":");
		message.append("\nYou have " + (gains > 0 ? "win" : "lost") + " " + Math.abs(gains) + " coins !");
		BotUtils.sendMessage(message.toString(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Play a game of slot machine for " + PAID_COST + " coins.**")
				.appendField("Cost", "A game costs " + PAID_COST + " coins.", false)
				.appendField("Gains", "You have a 12.5% chance of winning 30 coins, a 5.3% chance of winning 150 coins and a 0.2% chance of winning 3000 coins.", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
