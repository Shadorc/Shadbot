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
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.IMAGE, names = { "suicide_girls", "suicide-girls", "suicidegirls" }, alias = "sg")
public class SuicideGirlsCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		return context.isChannelNsfw()
				.filter(Boolean.TRUE::equals)
				.map(isNsfw -> {
					try {
						final Document doc = NetUtils.getDoc("https://www.suicidegirls.com/photos/sg/recent/all/");

						final Element girl = Utils.randValue(doc.getElementsByTag("article"));
						final String name = girl.getElementsByTag("a").attr("href").split("/")[2].trim();
						final String imageUrl = girl.select("noscript").attr("data-retina");
						final String url = girl.getElementsByClass("facebook-share").attr("href");

						return EmbedUtils.getDefaultEmbed()
								.setAuthor("SuicideGirls Image",
										"https://upload.wikimedia.org/wikipedia/commons/thumb/0/05/SuicideGirls_logo.svg/1280px-SuicideGirls_logo.svg.png",
										url)
								.setDescription(String.format("Name: **%s**", StringUtils.capitalize(name)))
								.setImage(imageUrl);
					} catch (IOException err) {
						loadingMsg.stopTyping();
						throw Exceptions.propagate(err);
					}
				})
				.flatMap(loadingMsg::send)
				.switchIfEmpty(BotUtils.sendMessage(TextUtils.mustBeNsfw(context.getPrefix()), context.getChannel()))
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show a random image from SuicideGirls website.")
				.build();
	}

}
