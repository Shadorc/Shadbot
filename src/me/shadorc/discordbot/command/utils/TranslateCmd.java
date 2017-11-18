package me.shadorc.discordbot.command.utils;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.ExceptionUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.StringUtils;
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

	public TranslateCmd() {
		super(CommandCategory.UTILS, Role.USER, RateLimiter.DEFAULT_COOLDOWN, "translate", "translation", "trans");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		List<String> args = new ArrayList<>(Arrays.asList(StringUtils.getSplittedArg(context.getArg(), 3)));
		if(args.size() < 2) {
			throw new MissingArgumentException();
		}

		if(args.size() == 2) {
			args.add(0, "auto");
		}

		String langFrom = this.toISO(args.get(0).toLowerCase());
		String langTo = this.toISO(args.get(1).toLowerCase());

		if(langFrom == null || langTo == null) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " One of the specified language doesn't exist."
					+ " Use `" + context.getPrefix() + "help " + this.getFirstName() + "` to see a complete list of supported languages.", context.getChannel());
			return;
		}

		String sourceText = args.get(2);
		try {
			JSONArray result = new JSONArray(NetUtils.getBody("https://translate.googleapis.com/translate_a/single?"
					+ "client=gtx"
					+ "&sl=" + URLEncoder.encode(langFrom, "UTF-8")
					+ "&tl=" + URLEncoder.encode(langTo, "UTF-8")
					+ "&dt=t&q=" + URLEncoder.encode(sourceText, "UTF-8")));

			if(!(result.get(0) instanceof JSONArray)) {
				BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " One of the specified language isn't supported."
						+ " Use `" + context.getPrefix() + "help " + this.getFirstName() + "` to see a complete list of supported languages.", context.getChannel());
				return;
			}

			String translatedText = ((JSONArray) ((JSONArray) result.get(0)).get(0)).get(0).toString();
			BotUtils.sendMessage(Emoji.MAP + " Translation: " + translatedText, context.getChannel());

		} catch (JSONException | IOException err) {
			ExceptionUtils.manageException("getting translation", context, err);
		}
	}

	private String toISO(String lang) {
		if("auto".equals(lang) || LANG_ISO_MAP.containsValue(lang)) {
			return lang;
		} else {
			return LANG_ISO_MAP.get(lang);
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Translate a text from a language to another.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " [<lang1>] <lang2> <text>`", false)
				.appendField("Arguments", "**lang1** - [OPTIONAL] source language, by leaving it blank the language will be automatically detected"
						+ "\n**lang2** - destination language", false)
				.appendField("Documentation", "List of supported languages: https://cloud.google.com/translate/docs/languages", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
