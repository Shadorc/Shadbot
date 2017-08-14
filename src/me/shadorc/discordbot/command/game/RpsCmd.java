package me.shadorc.discordbot.command.game;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.MathUtils;
import sx.blah.discord.util.EmbedBuilder;

public class RpsCmd extends Command {

	private interface Handsign {
		String ROCK = "rock";
		String PAPER = "paper";
		String SCISSORS = "scissors";
	}

	private static final List<String> HANDSIGNS = Arrays.asList(Handsign.ROCK, Handsign.PAPER, Handsign.SCISSORS);
	private static final List<String> EMOJIS = Arrays.asList(Emoji.GEM, Emoji.LEAF, Emoji.SCISSORS);
	private static final int GAINS = 20;

	public RpsCmd() {
		super(false, "rps");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(context.getArg() == null) {
			throw new MissingArgumentException();
		}

		String userHandsign = context.getArg().toLowerCase();

		if(!HANDSIGNS.contains(userHandsign)) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " Invalid handsign, please use \"rock\", \"paper\" or \"scissors\".", context.getChannel());
			return;
		}

		String botHandsign = HANDSIGNS.get(MathUtils.rand(HANDSIGNS.size()));

		String userHandsignForm = EMOJIS.get(HANDSIGNS.indexOf(userHandsign)) + " " + StringUtils.capitalize(userHandsign);
		String botHandsignForm = EMOJIS.get(HANDSIGNS.indexOf(botHandsign)) + " " + StringUtils.capitalize(botHandsign);

		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("**" + context.getAuthorName() + "**: " + userHandsignForm + ".\n");
		strBuilder.append("**Shadbot**: " + botHandsignForm + ".\n");

		if(userHandsign.equals(botHandsign)) {
			strBuilder.append("It's a draw !");
		} else if(userHandsign.equals(Handsign.ROCK) && botHandsign.equals(Handsign.SCISSORS)
				|| userHandsign.equals(Handsign.PAPER) && botHandsign.equals(Handsign.ROCK)
				|| userHandsign.equals(Handsign.SCISSORS) && botHandsign.equals(Handsign.PAPER)) {
			strBuilder.append(context.getAuthorName() + " wins ! Well done, you won " + GAINS + " coins.");
			context.getUser().addCoins(GAINS);
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
