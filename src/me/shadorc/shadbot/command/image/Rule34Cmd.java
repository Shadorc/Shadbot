package me.shadorc.shadbot.command.image;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.IMAGE, names = { "rule34" }, alias = "r34")
public class Rule34Cmd extends AbstractCommand {

	private static final int MAX_TAGS_LENGTH = 400;

	@Override
	public void execute(Context context) {
		context.requireArg();

		context.isChannelNsfw().subscribe(isNsfw -> {
			if(!isNsfw) {
				BotUtils.sendMessage(TextUtils.mustBeNsfw(context.getPrefix()), context.getChannel());
				return;
			}

			LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

			try {
				String arg = context.getArg().get();
				String url = String.format("https://rule34.xxx/index.php?page=dapi&s=post&q=index&tags=%s",
						NetUtils.encode(arg.replace(" ", "_")));
				JSONObject mainObj = XML.toJSONObject(NetUtils.getBody(url));

				JSONObject postsObj = mainObj.getJSONObject("posts");
				if(postsObj.getInt("count") == 0) {
					loadingMsg.send(TextUtils.noResult(arg));
					return;
				}

				JSONObject postObj;
				if(postsObj.get("post") instanceof JSONArray) {
					JSONArray postsArray = postsObj.getJSONArray("post");
					postObj = postsArray.getJSONObject(ThreadLocalRandom.current().nextInt(postsArray.length()));
				} else {
					postObj = postsObj.getJSONObject("post");
				}

				List<String> tags = StringUtils.split(postObj.getString("tags"), " ");
				if(postObj.getBoolean("has_children") || tags.stream().anyMatch(tag -> tag.contains("loli") || tag.contains("shota"))) {
					loadingMsg.send(Emoji.WARNING + " Sorry, I don't display images containing children or tagged with `loli` or `shota`.");
					return;
				}

				context.getAuthorAvatarUrl().subscribe(avatarUrl -> {
					final String formattedtags = StringUtils.truncate(
							FormatUtils.format(tags, tag -> String.format("`%s`", tag.toString()), " "), MAX_TAGS_LENGTH);
					EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed()
							.setAuthor(String.format("Rule34 (Search: %s)", context.getArg().get()),
									postObj.getString("file_url"),
									avatarUrl)
							.setThumbnail("http://rule34.paheal.net/themes/rule34v2/rule34_logo_top.png")
							.addField("Resolution", String.format("%dx%s", postObj.getInt("width"), postObj.getInt("height")), false)
							.addField("Tags", formattedtags, false)
							.setImage(postObj.getString("file_url"))
							.setFooter("If there is no preview, click on the title to see the media (probably a video)", null);

					String source = postObj.get("source").toString();
					if(!source.isEmpty()) {
						embed.setDescription(String.format("%n[**Source**](%s)", source));
					}

					loadingMsg.send(embed);
				});

			} catch (JSONException | IOException err) {
				loadingMsg.stopTyping();
				throw Exceptions.propagate(err);
			}
		});
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show a random image corresponding to a tag from Rule34 website.")
				.addArg("tag", false)
				.build();
	}
}
