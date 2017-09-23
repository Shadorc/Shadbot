package me.shadorc.discordbot.command.image;

import java.io.IOException;
import java.time.temporal.ChronoUnit;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.data.Storage.Setting;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class SuicideGirlsCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public SuicideGirlsCmd() {
		super(CommandCategory.IMAGE, Role.USER, "suicide_girls", "suicide-girls", "suicidegirls", "sg");
		this.rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		if(!context.getChannel().isNSFW()) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " This must be a NSFW-channel. If you're an admin, you can use "
					+ "`" + context.getPrefix() + "settings " + Setting.NSFW + " toggle`", context.getChannel());
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
					.withUrl(url)
					.withColor(Config.BOT_COLOR)
					.appendDescription("Girl: **" + StringUtils.capitalize(name) + "**")
					.withImage(imageUrl);

			BotUtils.sendEmbed(embed.build(), context.getChannel());
		} catch (IOException err) {
			LogUtils.error("Something went wrong while getting SuicideGirls image... Please, try again later.", err, context);
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show a random image from SuicideGirls website.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
