package me.shadorc.shadbot.command.image;

import java.util.List;
import java.util.function.Consumer;

import org.json.XML;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.api.image.r34.R34Post;
import me.shadorc.shadbot.api.image.r34.R34Posts;
import me.shadorc.shadbot.api.image.r34.R34Response;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.publisher.Mono;

public class Rule34Cmd extends BaseCmd {

	private static final int MAX_TAGS_LENGTH = 400;

	public Rule34Cmd() {
		super(CommandCategory.IMAGE, List.of("rule34"), "r34");
		this.setDefaultRateLimiter();
	}

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();
		final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		return context.isChannelNsfw()
				.flatMap(isNsfw -> Mono.fromCallable(() -> {
					if(!isNsfw) {
						return loadingMsg.setContent(TextUtils.mustBeNsfw(context.getPrefix()));
					}

					final String url = String.format("https://rule34.xxx/index.php?page=dapi&s=post&q=index&tags=%s",
							NetUtils.encode(arg.replace(" ", "_")));

					final R34Response r34 = Utils.MAPPER.readValue(XML.toJSONObject(NetUtils.getBody(url)).toString(), R34Response.class);
					final R34Posts posts = r34.getPosts();

					if(posts == null || posts.getCount() == 0) {
						return loadingMsg.setContent(String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) No images were found for the search `%s`",
								context.getUsername(), arg));
					}

					final R34Post post = Utils.randValue(posts.getPosts());

					final List<String> tags = StringUtils.split(post.getTags(), " ");
					if(post.hasChildren() || tags.stream().anyMatch(tag -> tag.contains("loli") || tag.contains("shota"))) {
						return loadingMsg.setContent(
								String.format(Emoji.WARNING + " (**%s**) I don't display images containing children or tagged with `loli` or `shota`.",
										context.getUsername()));
					}

					final String formattedtags = org.apache.commons.lang3.StringUtils.truncate(
							FormatUtils.format(tags, tag -> String.format("`%s`", tag), " "), MAX_TAGS_LENGTH);

					return loadingMsg.setEmbed(EmbedUtils.getDefaultEmbed()
							.andThen(embed -> {
								embed.setAuthor(String.format("Rule34: %s", arg), post.getFileUrl(), context.getAvatarUrl())
										.setThumbnail("http://rule34.paheal.net/themes/rule34v2/rule34_logo_top.png")
										.addField("Resolution", String.format("%dx%s", post.getWidth(), post.getHeight()), false)
										.addField("Tags", formattedtags, false)
										.setImage(post.getFileUrl())
										.setFooter("If there is no preview, click on the title to see the media (probably a video)", null);

								if(!post.getSource().isEmpty()) {
									embed.setDescription(String.format("%n[**Source**](%s)", post.getSource()));
								}
							}));
				}))
				.flatMap(LoadingMessage::send)
				.doOnTerminate(loadingMsg::stopTyping)
				.then();
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show a random image corresponding to a tag from Rule34 website.")
				.setSource("https://www.rule34.xxx/")
				.addArg("tag", false)
				.build();
	}
}
