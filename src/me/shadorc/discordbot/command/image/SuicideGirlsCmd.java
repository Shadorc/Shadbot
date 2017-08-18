package me.shadorc.discordbot.command.image;

import java.io.IOException;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.StringUtils;
import sx.blah.discord.util.EmbedBuilder;

public class SuicideGirlsCmd extends AbstractCommand {

	public SuicideGirlsCmd() {
		super(Role.USER, "suicidegirls", "sg");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.getChannel().isNSFW()) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " This must be a NSFW-channel.", context.getChannel());
			return;
		}

		try {
			Document doc = NetUtils.getDoc("https://www.suicidegirls.com/photos/sg/recent/all/");

			Elements elementsGirls = doc.getElementsByTag("article");
			Element girl = elementsGirls.get(MathUtils.rand(elementsGirls.size()));

			String name = girl.getElementsByTag("a").attr("href").split("/")[2].trim();
			String imageUrl = girl.select("noscript").attr("data-retina");
			String url = girl.getElementsByClass("facebook-share").attr("href");

			EmbedBuilder embed = new EmbedBuilder()
					.withAuthorName("SuicideGirls Image")
					.withAuthorIcon("https://upload.wikimedia.org/wikipedia/commons/thumb/0/05/SuicideGirls_logo.svg/1280px-SuicideGirls_logo.svg.png")
					.withColor(Config.BOT_COLOR)
					.withUrl(url)
					.withDescription("Girl: **" + StringUtils.capitalize(name) + "**")
					.withImage(imageUrl);

			BotUtils.sendEmbed(embed.build(), context.getChannel());
		} catch (IOException e) {
			LogUtils.error("Something went wrong while getting SuicideGirls image... Please, try again later.", e, context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show a random image from SuicideGirls website.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
