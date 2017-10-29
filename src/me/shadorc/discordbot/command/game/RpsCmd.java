package me.shadorc.discordbot.command.game;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.StatCategory;
import me.shadorc.discordbot.data.Stats;
import me.shadorc.discordbot.data.Storage;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class RpsCmd extends AbstractCommand {

	private static final int GAINS = 170;

	private final RateLimiter rateLimiter;

	private enum Handsign {
		ROCK("Rock", Emoji.GEM),
		PAPER("Paper", Emoji.LEAF),
		SCISSORS("Scissors", Emoji.SCISSORS);

		private String handsign;
		private Emoji emoji;

		Handsign(String handsign, Emoji emoji) {
			this.handsign = handsign;
			this.emoji = emoji;
		}

		public String getValue() {
			return handsign;
		}

		@Override
		public String toString() {
			return emoji + " " + handsign;
		}

		public static Handsign getEnum(String value) {
			for(Handsign handsign : Handsign.values()) {
				if(handsign.getValue().equalsIgnoreCase(value)) {
					return handsign;
				}
			}
			return null;
		}
	}

	public RpsCmd() {
		super(CommandCategory.GAME, Role.USER, "rps");
		this.rateLimiter = new RateLimiter(RateLimiter.GAME_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		Handsign userHandsign = Handsign.getEnum(context.getArg());

		if(userHandsign == null) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid handsign, please use \"rock\", \"paper\" or \"scissors\".", context.getChannel());
			return;
		}

		Handsign botHandsign = Arrays.asList(Handsign.values()).get(MathUtils.rand(Handsign.values().length));

		StringBuilder strBuilder = new StringBuilder("**" + context.getAuthorName() + "**: " + userHandsign.toString() + ".\n"
				+ "**Shadbot**: " + botHandsign.toString() + ".\n");

		if(userHandsign.equals(botHandsign)) {
			strBuilder.append("It's a draw !");
		} else if(userHandsign.equals(Handsign.ROCK) && botHandsign.equals(Handsign.SCISSORS)
				|| userHandsign.equals(Handsign.PAPER) && botHandsign.equals(Handsign.ROCK)
				|| userHandsign.equals(Handsign.SCISSORS) && botHandsign.equals(Handsign.PAPER)) {
			strBuilder.append(context.getAuthorName() + " wins ! Well done, you won **" + GAINS + " coins**.");
			Storage.addCoins(context.getGuild(), context.getAuthor(), GAINS);
			Stats.increment(StatCategory.MONEY_GAINS_COMMAND, this.getNames()[0], GAINS);
		} else {
			strBuilder.append(Shadbot.getClient().getOurUser().getName() + " wins !");
		}

		BotUtils.sendMessage(strBuilder.toString(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Play a Rock–paper–scissors game.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getNames()[0] + " <handsign>`", false)
				.appendField("Argument", "**handsign** -  rock, paper or scissors", false)
				.appendField("Gains", "The winner gets **" + GAINS + " coins**.", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

}
