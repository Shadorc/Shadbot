package me.shadorc.discordbot.command.utils;

import java.io.IOException;
import java.time.temporal.ChronoUnit;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.util.EmbedBuilder;

public class TranslateCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public TranslateCmd() {
		super(false, "translate", "trans", "traduire", "trad");
		this.rateLimiter = new RateLimiter(2, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		if(rateLimiter.isLimited(context.getGuild(), context.getAuthor())) {
			if(!rateLimiter.isWarned(context.getGuild(), context.getAuthor())) {
				rateLimiter.warn("Take it easy, don't spam :)", context);
			}
			return;
		}

		String[] args = context.getArg().split(" ", 3);
		if(args.length < 2) {
			throw new MissingArgumentException();
		}

		String lang1 = "";
		String lang2 = "";
		String text = "";

		if(args.length == 2) {
			lang1 = "auto";
			lang2 = args[0].toLowerCase();
			text = args[1];
		} else {
			lang1 = args[0].toLowerCase();
			lang2 = args[1].toLowerCase();
			text = args[2];
		}

		if(lang1.equals(lang2)) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " The source language and the targetted language must be different.", context.getChannel());
			return;
		}

		try {
			String translatedText = Utils.translate(lang1, lang2, text);
			BotUtils.sendMessage(Emoji.MAP + " Translation : " + translatedText, context.getChannel());
		} catch (IllegalArgumentException argErr) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " One of the specified language isn't supported or doesn't exist. Use " + context.getPrefix() + "help translate to see a complete list of supported languages.", context.getChannel());
		} catch (IOException ioErr) {
			LogUtils.error("Something went wrong during translation... Please, try again later.", ioErr, context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Translate a text from a language to another.**")
				.appendField("Arguments", "<lang1> is optional, by leaving it blank the source language will be automatically detected.", false)
				.appendField("Documentation", "List of supported languages : https://cloud.google.com/translate/docs/languages", false)
				.appendField("Usage", context.getPrefix() + "translate [<lang1>] <lang2> <text>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
