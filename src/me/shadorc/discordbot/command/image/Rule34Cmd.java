package me.shadorc.discordbot.command.image;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.temporal.ChronoUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.Storage.Setting;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class Rule34Cmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public Rule34Cmd() {
		super(CommandCategory.IMAGE, Role.USER, "rule34", "r34");
		this.rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		if(!context.getChannel().isNSFW()) {
			BotUtils.send(Emoji.GREY_EXCLAMATION + " This must be a NSFW-channel. If you're an admin, you can use "
					+ "`" + context.getPrefix() + "settings " + Setting.NSFW + " toggle`", context.getChannel());
			return;
		}

		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		try {
			JSONObject mainObj = XML.toJSONObject(NetUtils.getBody("https://rule34.xxx/index.php?"
					+ "page=dapi"
					+ "&s=post"
					+ "&q=index"
					+ "&tags=" + URLEncoder.encode(context.getArg(), "UTF-8")));

			JSONObject postsObj = mainObj.getJSONObject("posts");

			if(postsObj.getInt("count") == 0) {
				BotUtils.send(Emoji.MAGNIFYING_GLASS + " No result for \"" + context.getArg() + "\".", context.getChannel());
				return;
			}

			JSONObject postObj;
			if(postsObj.get("post") instanceof JSONArray) {
				JSONArray postsArray = postsObj.getJSONArray("post");
				postObj = postsArray.getJSONObject(MathUtils.rand(postsArray.length() - 1));
			} else {
				postObj = postsObj.getJSONObject("post");
			}

			String tags = postObj.getString("tags").trim().replace(" ", ", ");
			if(tags.length() > 400) {
				tags = tags.substring(0, 400) + "...";
			}

			StringBuilder fileUrl = new StringBuilder(postObj.getString("file_url"));
			if(!fileUrl.toString().startsWith("http")) {
				fileUrl.insert(0, "http:");
			}

			StringBuilder sourceUrl = new StringBuilder(postObj.getString("source"));
			if(!sourceUrl.toString().startsWith("http")) {
				sourceUrl.insert(0, "http:");
			}

			EmbedBuilder embed = Utils.getDefaultEmbed()
					.setLenient(true)
					.withAuthorName("Rule34 (Search: " + context.getArg() + ")")
					.withUrl(fileUrl.toString())
					.withThumbnail("http://rule34.paheal.net/themes/rule34v2/rule34_logo_top.png")
					.appendField("Resolution", postObj.getInt("width") + "x" + postObj.getInt("height"), false)
					.appendField("Source", sourceUrl.toString(), false)
					.appendField("Tags", tags, false)
					.withImage(fileUrl.toString())
					.withFooterText("If there is no preview, click on the title to see the media (probably a video)");
			BotUtils.send(embed.build(), context.getChannel());

		} catch (JSONException | IOException err) {
			LogUtils.error("Something went wrong while getting an image from Rule34... Please, try again later.", err, context);
		}

	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show a random image corresponding to a tag from Rule34 website.**")
				.appendField("Usage", "`" + context.getPrefix() + "rule34 <tag(s)>`", false);
		BotUtils.send(builder.build(), context.getChannel());
	}

}
