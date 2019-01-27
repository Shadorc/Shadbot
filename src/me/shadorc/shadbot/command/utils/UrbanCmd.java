package me.shadorc.shadbot.command.utils;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import discord4j.core.object.Embed;
import discord4j.core.object.Embed.Field;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.api.urbandictionary.UrbanDefinition;
import me.shadorc.shadbot.api.urbandictionary.UrbanDictionaryResponse;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.UTILS, names = { "urban" }, alias = "ud")
public class UrbanCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			final URL url = new URL(String.format("https://api.urbandictionary.com/v0/define?term=%s", NetUtils.encode(arg)));

			final UrbanDictionaryResponse urbanDictionary = Utils.MAPPER.readValue(url, UrbanDictionaryResponse.class);

			if(urbanDictionary.getDefinitions().isEmpty()) {
				return loadingMsg.send(String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) No urban definitions found for `%s`",
						context.getUsername(), arg))
						.then();
			}

			final UrbanDefinition urbanDefinition = urbanDictionary.getDefinitions().get(0);

			final String definition = StringUtils.abbreviate(urbanDefinition.getDefinition(), Embed.MAX_DESCRIPTION_LENGTH);
			final String example = StringUtils.abbreviate(urbanDefinition.getExample(), Field.MAX_VALUE_LENGTH);

			return context.getAvatarUrl()
					.map(avatarUrl -> {
						final Consumer<? super EmbedCreateSpec> embedConsumer = embed -> {
							EmbedUtils.getDefaultEmbed().accept(embed);
							embed.setAuthor(String.format("Urban Dictionary: %s", urbanDefinition.getWord()), urbanDefinition.getPermalink(), avatarUrl)
								.setThumbnail("http://www.packal.org/sites/default/files/public/styles/icon_large/public/workflow-files/florianurban/icon/icon.png")
								.setDescription(definition)
								.addField("Example", example, false);
						};
						
						return embedConsumer;
					})
					.flatMap(loadingMsg::send)
					.then();

		} catch (final IOException err) {
			loadingMsg.stopTyping();
			throw Exceptions.propagate(err);
		}

	}

	@Override
	public Mono<Consumer<? super EmbedCreateSpec>> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show Urban Dictionary definition for a search.")
				.addArg("search", false)
				.build();
	}

}
