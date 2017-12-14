package me.shadorc.discordbot.command.image;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.Setting;
import me.shadorc.discordbot.exceptions.MissingArgumentException;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.ExceptionUtils;
import me.shadorc.discordbot.utils.FormatUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.TextUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class Rule34Cmd extends AbstractCommand {

	private static final int MAX_TAGS_LENGTH = 400;

	public Rule34Cmd() {
		super(CommandCategory.IMAGE, Role.USER, RateLimiter.DEFAULT_COOLDOWN, "rule34");
		this.setAlias("r34");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.getChannel().isNSFW()) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " This must be a NSFW-channel. If you're an admin, you can use "
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
					+ "&tags=" + URLEncoder.encode(context.getArg().replace(" ", "_"), "UTF-8")));

			JSONObject postsObj = mainObj.getJSONObject("posts");

			if(postsObj.getInt("count") == 0) {
				BotUtils.sendMessage(TextUtils.noResult(context.getArg()), context.getChannel());
				return;
			}

			JSONObject postObj;
			if(postsObj.get("post") instanceof JSONArray) {
				JSONArray postsArray = postsObj.getJSONArray("post");
				postObj = postsArray.getJSONObject(MathUtils.rand(postsArray.length() - 1));
			} else {
				postObj = postsObj.getJSONObject("post");
			}

			String[] tags = postObj.getString("tags").trim().split(" ");

			if(postObj.getBoolean("has_children") || this.isNotLegal(tags)) {
				BotUtils.sendMessage(Emoji.WARNING + " Sorry, I don't display images containing children or tagged with `loli` or `shota`.", context.getChannel());
				return;
			}

			String formattedtags = FormatUtils.formatArray(tags, tag -> "`" + tag.toString().trim() + "`", " ");
			if(formattedtags.length() > MAX_TAGS_LENGTH) {
				formattedtags = formattedtags.substring(0, MAX_TAGS_LENGTH - 3) + "...";
			}

			String fileUrl = this.getValidURL(postObj.getString("file_url"));
			String sourceUrl = this.getValidURL(postObj.get("source").toString());

			EmbedBuilder embed = Utils.getDefaultEmbed()
					.setLenient(true)
					.withAuthorName("Rule34 (Search: " + context.getArg() + ")")
					.withUrl(fileUrl)
					.withThumbnail("http://rule34.paheal.net/themes/rule34v2/rule34_logo_top.png")
					.appendField("Resolution", postObj.getInt("width") + "x" + postObj.getInt("height"), false)
					.appendField("Source", sourceUrl, false)
					.appendField("Tags", formattedtags, false)
					.withImage(fileUrl)
					.withFooterText("If there is no preview, click on the title to see the media (probably a video)");
			BotUtils.sendMessage(embed.build(), context.getChannel());

		} catch (JSONException | IOException err) {
			ExceptionUtils.manageException("getting an image from Rule34", context, err);
		}
	}

	private String getValidURL(String url) {
		if(url == null || !url.startsWith("//")) {
			return url;
		}
		return "http:" + url;
	}

	private boolean isNotLegal(String... tags) {
		return Arrays.asList(tags).stream().anyMatch(tag -> tag.contains("loli") || tag.contains("shota"));
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show a random image corresponding to a tag from Rule34 website.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " <tag>`", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
