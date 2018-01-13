package me.shadorc.shadbot.command.image;

import java.io.IOException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.MathUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.LoadingMessage;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

@RateLimited
@Command(category = CommandCategory.IMAGE, names = { "rule34" }, alias = "r34")
public class Rule34Cmd extends AbstractCommand {

	private static final int MAX_TAGS_LENGTH = 400;

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.getChannel().isNSFW()) {
			BotUtils.sendMessage(TextUtils.mustBeNSFW(context.getPrefix()), context.getChannel());
			return;
		}

		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		LoadingMessage loadingMsg = new LoadingMessage("Loading image...", context.getChannel());
		loadingMsg.send();

		try {
			String url = String.format("https://rule34.xxx/index.php?page=dapi&s=post&q=index&tags=%s",
					NetUtils.encode(context.getArg().replace(" ", "_")));
			JSONObject mainObj = XML.toJSONObject(NetUtils.getBody(url));

			JSONObject postsObj = mainObj.getJSONObject("posts");
			if(postsObj.getInt("count") == 0) {
				loadingMsg.edit(TextUtils.noResult(context.getArg()));
				return;
			}

			JSONObject postObj;
			if(postsObj.get("post") instanceof JSONArray) {
				JSONArray postsArray = postsObj.getJSONArray("post");
				postObj = postsArray.getJSONObject(MathUtils.rand(postsArray.length()));
			} else {
				postObj = postsObj.getJSONObject("post");
			}

			List<String> tags = StringUtils.split(postObj.getString("tags"), " ");
			if(postObj.getBoolean("has_children") || tags.stream().anyMatch(tag -> tag.contains("loli") || tag.contains("shota"))) {
				loadingMsg.edit(Emoji.WARNING + " Sorry, I don't display images containing children or tagged with `loli` or `shota`.");
				return;
			}

			String formattedtags = StringUtils.truncate(
					FormatUtils.format(tags, tag -> String.format("`%s`", tag.toString()), " "), MAX_TAGS_LENGTH);
			EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
					.setLenient(true)
					.withAuthorName("Rule34 (Search: " + context.getArg() + ")")
					.withUrl(postObj.getString("file_url"))
					.withThumbnail("http://rule34.paheal.net/themes/rule34v2/rule34_logo_top.png")
					.withDescription(String.format("%n[**Source**](%s)", postObj.get("source").toString()))
					.appendField("Resolution", String.format("%dx%s", postObj.getInt("width"), postObj.getInt("height")), false)
					.appendField("Tags", formattedtags, false)
					.withImage(postObj.getString("file_url"))
					.withFooterText("If there is no preview, click on the title to see the media (probably a video)");
			loadingMsg.edit(embed.build());

		} catch (JSONException | IOException err) {
			Utils.handle("getting an image from Rule34", context, err);
		}
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show a random image corresponding to a tag from Rule34 website.")
				.addArg("tag", false)
				.build();
	}
}
