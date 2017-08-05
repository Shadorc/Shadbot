package me.shadorc.discordbot.command.utils;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.Log;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.util.EmbedBuilder;

public class TranslateCmd extends Command {

	public TranslateCmd() {
		super(false, "trad", "translate", "traduire");
	}

	@Override
	public void execute(Context context) {
		// Country doc https://ctrlq.org/code/19899-google-translate-languages
		if(context.getArg() == null) {
			throw new IllegalArgumentException();
		}

		String[] args = context.getArg().split(" ", 3);
		if(args.length < 3) {
			throw new IllegalArgumentException();
		}

		try {
			String word = Utils.translate(args[0], args[1], args[2]);
			BotUtils.sendMessage(Emoji.MAP + " Translation : " + word, context.getChannel());
		} catch (Exception e) {
			Log.error("An error occured during the translation.", e, context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for /" + this.getNames()[0])
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Translate a text from a language to another.**")
				.appendField("Usage", "/translate <lang1> <lang2> <text>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
