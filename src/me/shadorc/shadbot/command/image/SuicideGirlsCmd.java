package me.shadorc.shadbot.command.image;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.IMAGE, names = { "suicide_girls", "suicide-girls", "suicidegirls" }, alias = "sg")
public class SuicideGirlsCmd extends AbstractCommand {

	@Override
	public void execute(Context context) {
		context.isChannelNsfw().subscribe(isNsfw -> {
			if(!isNsfw) {
				BotUtils.sendMessage(TextUtils.mustBeNsfw(context.getPrefix()), context.getChannel());
				return;
			}

			LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

			try {
				Document doc = NetUtils.getDoc("https://www.suicidegirls.com/photos/sg/recent/all/");

				Elements girls = doc.getElementsByTag("article");
				Element girl = girls.get(ThreadLocalRandom.current().nextInt(girls.size()));

				String name = girl.getElementsByTag("a").attr("href").split("/")[2].trim();
				String imageUrl = girl.select("noscript").attr("data-retina");
				String url = girl.getElementsByClass("facebook-share").attr("href");

				EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed("SuicideGirls Image",
						"https://upload.wikimedia.org/wikipedia/commons/thumb/0/05/SuicideGirls_logo.svg/1280px-SuicideGirls_logo.svg.png",
						url)
						.setDescription(String.format("Name: **%s**", StringUtils.capitalize(name)))
						.setImage(imageUrl);

				loadingMsg.send(embed);
			} catch (IOException err) {
				loadingMsg.send(ExceptionUtils.handleAndGet("getting SuicideGirls image", context, err));
			}
		});
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show a random image from SuicideGirls website.")
				.build();
	}

}
