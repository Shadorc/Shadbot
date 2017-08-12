package me.shadorc.discordbot.command.utils;

import java.io.IOException;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.Log;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.util.EmbedBuilder;

public class TranslateCmd extends Command {

	public TranslateCmd() {
		super(false, "translate", "trans", "traduire", "trad");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(context.getArg() == null) {
			throw new MissingArgumentException();
		}

		String[] args = context.getArg().split(" ", 3);
		if(args.length < 2) {
			throw new MissingArgumentException();
		}

		if(args.length == 3 && args[0].equalsIgnoreCase(args[1])) {
			BotUtils.sendMessage(Emoji.WARNING + " The source language and the targetted language must be different.", context.getChannel());
			return;
		}

		String sourceLang = (args.length == 3) ? args[0] : "auto";
		String targetLang = (args.length == 3) ? args[1] : args[0];
		String sourceText = (args.length == 3) ? args[2] : args[1];

		try {
			String translatedText = Utils.translate(sourceLang, targetLang, sourceText);
			BotUtils.sendMessage(Emoji.MAP + " Translation : " + translatedText, context.getChannel());
		} catch (IllegalArgumentException argErr) {
			BotUtils.sendMessage(Emoji.WARNING + " One of the specified language isn't supported or doesn't exist. Use /help translate to see a complete list of supported languages.", context.getChannel());
		} catch (IOException ioErr) {
			Log.error("An error occured during the translation.", ioErr, context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for /" + this.getNames()[0])
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Translate a text from a language to another.**")
				.appendField("Arguments", "<lang1> is optional, by leaving it blank the source language will be automatically detected.", false)
				.appendField("Documentation", "List of supported languages : https://cloud.google.com/translate/docs/languages", false)
				.appendField("Usage", "/translate [<lang1>] <lang2> <text>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
