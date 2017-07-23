package me.shadorc.discordbot.command;

import java.awt.Color;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.utility.BotUtils;
import sx.blah.discord.util.EmbedBuilder;

public class HelpCmd extends Command {

	public HelpCmd() {
		super("help", "aide");
	}

	@Override
	public void execute(Context context) {
		//EmbedBuilder doc : https://discord4j.readthedocs.io/en/latest/Making-embedded-content-using-EmbedBuilder/
		EmbedBuilder builder = new EmbedBuilder();

		builder.withAuthorName("Shadbot Help");
		builder.withAuthorIcon(context.getClient().getOurUser().getAvatarURL());
		builder.withColor(new Color(170, 196, 222));
		builder.withDesc("Command help. For extended usage please use /help <command>.");
		builder.appendField("Utile", "`/trad <lang1> <lang2> <texte>`"
				+ "\n`/wiki <recherche>`" 
				+ "\n`/vacances <zone>`"
				+ "\n`/calc <calcul>`"
				+ "\n`/meteo <ville>`", false);
		builder.appendField("Fun", "`/chat <message>`"
				+ "\n`/gif <tag>`"
				+ "\n`/gif`"
				+ "\n`/dtc`"
				+ "\n`/blague`", false);
		builder.appendField("Coins", "`/roulette_russe`"
				+ "\n`/machine_sous`"
				+ "\n`/trivia`"
				+ "\n`/coin`s", false);
		builder.appendField("Music", "`/play <music>`"
				+ "\n`/music volume <0-100>`"
				+ "\n`/music pause`"
				+ "\n`/music stop`"
				+ "\n`/music next`"
				+ "\n`/music name`"
				+ "\n`/music playlist`"
				+ "\n`/leave`", false);

		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
