package me.shadorc.shadbot.command.image;

import java.io.IOException;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.MathUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

@RateLimited
@Command(category = CommandCategory.IMAGE, names = { "suicide_girls", "suicide-girls", "suicidegirls" }, alias = "sg")
public class SuicideGirlsCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.getChannel().isNSFW()) {
			BotUtils.sendMessage(TextUtils.mustBeNSFW(context.getPrefix()), context.getChannel());
			return;
		}

		try {
			Document doc = NetUtils.getDoc("https://www.suicidegirls.com/photos/sg/recent/all/");

			Elements girls = doc.getElementsByTag("article");
			Element girl = girls.get(MathUtils.rand(girls.size()));

			String name = girl.getElementsByTag("a").attr("href").split("/")[2].trim();
			String imageUrl = girl.select("noscript").attr("data-retina");
			String url = girl.getElementsByClass("facebook-share").attr("href");

			EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
					.withAuthorName("SuicideGirls Image")
					.withAuthorIcon("https://upload.wikimedia.org/wikipedia/commons/thumb/0/05/SuicideGirls_logo.svg/1280px-SuicideGirls_logo.svg.png")
					.withUrl(url)
					.appendDescription(String.format("Name: **%s**", StringUtils.capitalize(name)))
					.withImage(imageUrl);

			BotUtils.sendMessage(builder.build(), context.getChannel());
		} catch (IOException err) {
			ExceptionUtils.handle("getting SuicideGirls image", context, err);
		}
	}

	@Override
	public EmbedObject getHelp(Context context) {
		return new HelpBuilder(this, context.getPrefix())
				.setDescription("Show a random image from SuicideGirls website.")
				.build();
	}

}
