package me.shadorc.shadbot.command.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;

@RateLimited
@Command(category = CommandCategory.UTILS, names = { "translate", "translation", "trans" })
public class TranslateCmd extends AbstractCommand {

	private static final Map<String, String> LANG_ISO_MAP = new HashMap<>();

	static {
		for(String iso : Locale.getISOLanguages()) {
			LANG_ISO_MAP.put(new Locale(iso).getDisplayLanguage(Locale.ENGLISH).toLowerCase(), iso);
		}
	}

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalArgumentException {
		List<String> args = StringUtils.split(context.getArg(), 3);
		if(args.size() < 2) {
			throw new MissingArgumentException();
		}

		if(args.size() == 2) {
			args.add(0, "auto");
		}

		String langFrom = this.toISO(args.get(0));
		String langTo = this.toISO(args.get(1));

		if(langFrom == null || langTo == null) {
			throw new IllegalArgumentException("One of the specified language doesn't exist."
					+ " Use `" + context.getPrefix() + "help " + this.getName() + "` to see a complete list of supported languages.");
		}

		String sourceText = args.get(2);
		try {
			String url = String.format("https://translate.googleapis.com/translate_a/single?client=gtx&sl=%s&tl=%s&dt=t&q=%s",
					NetUtils.encode(langFrom),
					NetUtils.encode(langTo),
					NetUtils.encode(sourceText));
			JSONArray result = new JSONArray(NetUtils.getBody(url));

			if(!(result.get(0) instanceof JSONArray)) {
				throw new IllegalArgumentException("One of the specified language isn't supported. "
						+ String.format("Use `%shelp %s` to see a complete list of supported languages.", context.getPrefix(), this.getName()));
			}

			String translatedText = ((JSONArray) ((JSONArray) result.get(0)).get(0)).get(0).toString();
			BotUtils.sendMessage(Emoji.MAP + " Translation: " + translatedText, context.getChannel());

		} catch (JSONException | IOException err) {
			ExceptionUtils.handle("getting translation", context, err);
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
	public EmbedObject getHelp(Context context) {
		return new HelpBuilder(this, context.getPrefix())
				.setDescription("Translate a text from a language to another.")
				.addArg("lang1", "source language, by leaving it blank the language will be automatically detected", true)
				.addArg("lang2", "destination language", false)
				.appendField("Documentation", "List of supported languages: https://cloud.google.com/translate/docs/languages", false)
				.build();
	}
}
