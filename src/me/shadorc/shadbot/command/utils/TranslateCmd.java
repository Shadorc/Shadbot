package me.shadorc.shadbot.command.utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.UTILS, names = { "translate", "translation", "trans" })
public class TranslateCmd extends AbstractCommand {

	private static final String AUTO = "auto";
	private static final int CHARACTERS_LIMIT = 150;

	private static final BiMap<String, String> LANG_ISO_MAP = HashBiMap.create();

	static {
		Arrays.stream(Locale.getISOLanguages())
				.forEach(iso -> LANG_ISO_MAP.put(new Locale(iso).getDisplayLanguage(Locale.ENGLISH).toLowerCase(), iso));
		LANG_ISO_MAP.put(AUTO, AUTO);
	}

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		final List<String> quotedWords = StringUtils.getQuotedElements(arg);
		if(quotedWords.size() != 1) {
			throw new CommandException("The text to translate cannot be empty and must be enclosed in quotation marks.");
		}
		final String sourceText = quotedWords.get(0);
		if(sourceText.length() > CHARACTERS_LIMIT) {
			throw new CommandException(String.format("The text to translate cannot exceed %d characters.", CHARACTERS_LIMIT));
		}

		final List<String> langs = StringUtils.split(StringUtils.remove(arg, sourceText, "\""));
		if(langs.isEmpty()) {
			throw new MissingArgumentException();
		}

		if(langs.size() == 1) {
			langs.add(0, AUTO);
		}

		final String langFrom = this.toISO(langs.get(0));
		final String langTo = this.toISO(langs.get(1));

		final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());
		try {
			final String url = String.format("https://translate.googleapis.com/translate_a/single?"
					+ "client=gtx"
					+ "&ie=UTF-8"
					+ "&oe=UTF-8"
					+ "&sl=%s"
					+ "&tl=%s"
					+ "&dt=t"
					+ "&q=%s",
					NetUtils.encode(langFrom), NetUtils.encode(langTo), NetUtils.encode(sourceText));

			final JSONArray result = new JSONArray(NetUtils.getJSON(url));

			if(langFrom == null || langTo == null || !(result.get(0) instanceof JSONArray)) {
				loadingMsg.stopTyping();
				throw new CommandException(String.format("One of the specified language isn't supported. "
						+ "Use `%shelp %s` to see a complete list of supported languages.", context.getPrefix(), this.getName()));
			}

			final StringBuilder translatedText = new StringBuilder();
			final JSONArray translations = result.getJSONArray(0);
			for(int i = 0; i < translations.length(); i++) {
				translatedText.append(translations.getJSONArray(i).getString(0));
			}

			if(translatedText.toString().equalsIgnoreCase(sourceText)) {
				loadingMsg.stopTyping();
				throw new CommandException(String.format("The text could not been translated. "
						+ "Check that the specified languages are supported, that the text is in the specified language "
						+ "and that the destination language is different from the source one. "
						+ "Use `%shelp %s` to see a complete list of supported languages.", context.getPrefix(), this.getName()));
			}

			return context.getAvatarUrl()
					.map(avatarUrl -> EmbedUtils.getDefaultEmbed()
							.setAuthor("Translation", null, avatarUrl)
							.setDescription(String.format("**%s**%n%s%n%n**%s**%n%s",
									StringUtils.capitalize(LANG_ISO_MAP.inverse().get(langFrom)), sourceText,
									StringUtils.capitalize(LANG_ISO_MAP.inverse().get(langTo)), translatedText.toString())))
					.flatMap(embed -> loadingMsg.send(embed))
					.then();

		} catch (IOException err) {
			loadingMsg.stopTyping();
			throw Exceptions.propagate(err);
		}
	}

	private String toISO(String lang) {
		return LANG_ISO_MAP.containsValue(lang) ? lang : LANG_ISO_MAP.get(lang);
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Translate a text from a language to another.")
				.addArg("fromLang", "source language, by leaving it blank the language will be automatically detected", true)
				.addArg("toLang", "destination language", false)
				.addArg("\"text\"", false)
				.setExample(String.format("`%s%s en fr \"How are you ?\"`", context.getPrefix(), this.getName()))
				.addField("Documentation", "List of supported languages: https://cloud.google.com/translate/docs/languages", false)
				.build();
	}
}
