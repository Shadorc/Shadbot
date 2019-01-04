package me.shadorc.shadbot.command.image;

import java.io.IOException;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.IMAGE, names = { "suicide_girls", "suicide-girls", "suicidegirls" }, alias = "sg")
public class SuicideGirlsCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		return context.isChannelNsfw()
				.filter(Boolean.TRUE::equals)
				.flatMap(isNsfw -> {
					try {
						final Document doc = NetUtils.getDoc("https://www.suicidegirls.com/photos/sg/recent/all/");

						final Element girl = Utils.randValue(doc.getElementsByTag("article"));
						final String name = girl.getElementsByTag("a").attr("href").split("/")[2].trim();
						final String imageUrl = girl.select("noscript").attr("data-retina");
						final String url = girl.getElementsByClass("facebook-share").attr("href");

						return context.getAvatarUrl()
								.map(avatarUrl -> EmbedUtils.getDefaultEmbed()
										.setAuthor("SuicideGirls", url, avatarUrl)
										.setDescription(String.format("Name: **%s**", StringUtils.capitalize(name)))
										.setImage(imageUrl));
					} catch (final IOException err) {
						loadingMsg.stopTyping();
						throw Exceptions.propagate(err);
					}
				})
				.flatMap(loadingMsg::send)
				.switchIfEmpty(context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(TextUtils.mustBeNsfw(context.getPrefix()), channel)))
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show a random Suicide Girl image.")
				.setSource("https://www.suicidegirls.com/")
				.build();
	}

}
