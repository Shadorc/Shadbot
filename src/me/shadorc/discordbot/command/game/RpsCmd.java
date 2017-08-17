package me.shadorc.discordbot.command.game;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.MathUtils;
import sx.blah.discord.util.EmbedBuilder;

public class RpsCmd extends AbstractCommand {

	private static final int GAINS = 20;

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
		super(Role.USER, "rps");
		this.rateLimiter = new RateLimiter(10, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		if(rateLimiter.isLimited(context.getGuild(), context.getAuthor())) {
			if(!rateLimiter.isWarned(context.getGuild(), context.getAuthor())) {
				rateLimiter.warn("You can play Rock-paper-scissors only once every " + rateLimiter.getTimeout() + " seconds.", context);
			}
			return;
		}

		Handsign userHandsign = Handsign.getEnum(context.getArg());

		if(userHandsign == null) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " Invalid handsign, please use \"rock\", \"paper\" or \"scissors\".", context.getChannel());
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
			strBuilder.append(context.getAuthorName() + " wins ! Well done, you won " + GAINS + " coins.");
			context.getPlayer().addCoins(GAINS);
		} else {
			strBuilder.append(Shadbot.getClient().getOurUser().getName() + " wins !");
		}

		BotUtils.sendMessage(strBuilder.toString(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Play a Rock–paper–scissors game.**")
				.appendField("Usage", "/rps <rock|paper|scissors>", false)
				.appendField("Gains", "The winner gets " + GAINS + " coins.", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
