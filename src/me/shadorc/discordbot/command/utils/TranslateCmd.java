package me.shadorc.discordbot.command.utils;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class TranslateCmd extends AbstractCommand {

	private static final Map<String, String> LANG_ISO_MAP = new HashMap<>();

	static {
		for(String iso : Locale.getISOLanguages()) {
			LANG_ISO_MAP.put(new Locale(iso).getDisplayLanguage(Locale.ENGLISH).toLowerCase(), iso);
		}
	}

	private final RateLimiter rateLimiter;

	public TranslateCmd() {
		super(CommandCategory.UTILS, Role.USER, "translate", "translation", "trans");
		this.rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		String[] args = context.getArg().split(" ", 3);
		if(args.length < 2) {
			throw new MissingArgumentException();
		}

		String langFrom = "";
		String langTo = "";
		String sourceText = "";

		if(args.length == 2) {
			langFrom = "auto";
			langTo = args[0].toLowerCase();
			sourceText = args[1];
		} else {
			langFrom = args[0].toLowerCase();
			langTo = args[1].toLowerCase();
			sourceText = args[2];
		}

		if(!this.isValidISOLanguage(langFrom)) {
			langFrom = this.toISO(langFrom);
		}
		if(!this.isValidISOLanguage(langTo)) {
			langTo = this.toISO(langTo);
		}

		if(langFrom == null || langTo == null) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " One of the specified language doesn't exist."
					+ " Use `" + context.getPrefix() + "help translate` to see a complete list of supported languages.", context.getChannel());
			return;
		}

		if(langFrom.equals(langTo)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " The source language and the targetted language must be different.", context.getChannel());
			return;
		}

		try {
			JSONArray result = new JSONArray(NetUtils.getBody("https://translate.googleapis.com/translate_a/single?"
					+ "client=gtx"
					+ "&sl=" + URLEncoder.encode(langFrom, "UTF-8")
					+ "&tl=" + URLEncoder.encode(langTo, "UTF-8")
					+ "&dt=t&q=" + URLEncoder.encode(sourceText, "UTF-8")));

			if(!(result.get(0) instanceof JSONArray)) {
				BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " One of the specified language isn't supported."
						+ " Use `" + context.getPrefix() + "help translate` to see a complete list of supported languages.", context.getChannel());
				return;
			}

			String translatedText = ((JSONArray) ((JSONArray) result.get(0)).get(0)).get(0).toString();
			BotUtils.sendMessage(Emoji.MAP + " Translation: " + translatedText, context.getChannel());

		} catch (JSONException | IOException err) {
			LogUtils.error("Something went wrong during translation... Please, try again later.", err, context);
		}
	}

	private boolean isValidISOLanguage(String iso) {
		return LANG_ISO_MAP.containsValue(iso);
	}

	private String toISO(String lang) {
		return LANG_ISO_MAP.get(lang);
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Translate a text from a language to another.**")
				.appendField("Usage", "`" + context.getPrefix() + "translate [<lang1>] <lang2> <text>`", false)
				.appendField("Arguments", "**lang1** - [OPTIONAL] source language, by leaving it blank the language will be automatically detected"
						+ "\n**lang2** - destination language", false)
				.appendField("Documentation", "List of supported languages: https://cloud.google.com/translate/docs/languages", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
