package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.json.image.r34.R34Post;
import com.shadorc.shadbot.api.json.image.r34.R34Posts;
import com.shadorc.shadbot.api.json.image.r34.R34Response;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.NetUtil;
import com.shadorc.shadbot.utils.RandUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class Rule34Cmd extends BaseCmd {

    private static final String HOME_URL = "https://rule34.xxx/index.php";
    private static final int MAX_TAGS_CHAR = 250;

    public Rule34Cmd() {
        super(CommandCategory.IMAGE, "rule34", "Show random image from Rule34");
        this.addOption("query", "Search for a Rule34 image", true, ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        final String query = context.getOptionAsString("query").orElseThrow();

        return context.isChannelNsfw()
                .flatMap(isNsfw -> {
                    if (!isNsfw) {
                        return context.createFollowupMessage(ShadbotUtil.mustBeNsfw());
                    }

                    return context.createFollowupMessage(Emoji.HOURGLASS + " (**%s**) Loading rule34 image...", context.getAuthorName())
                            .flatMap(messageId -> Rule34Cmd.getR34Post(query)
                                    .flatMap(post -> {
                                        // Don't post images containing children
                                        if (Rule34Cmd.containsChildren(post, post.getTags())) {
                                            return context.editFollowupMessage(messageId,
                                                    Emoji.WARNING + " (**%s**) I don't display images " +
                                                            "containing children or tagged with `loli` or `shota`.",
                                                    context.getAuthorName());
                                        }

                                        return context.editFollowupMessage(messageId,
                                                Rule34Cmd.formatEmbed(post, query, context.getAuthorAvatarUrl()));
                                    })
                                    .switchIfEmpty(context.editFollowupMessage(messageId,
                                            Emoji.MAGNIFYING_GLASS + " (**%s**) No images found matching query `%s`",
                                            context.getAuthorName(), query)));
                });
    }

    private static Mono<R34Post> getR34Post(final String tag) {
        final String url = String.format("%s?" +
                        "page=dapi" +
                        "&s=post" +
                        "&q=index" +
                        "&tags=%s",
                HOME_URL, NetUtil.encode(tag.replace(" ", "_")));

        return RequestHelper.fromUrl(url)
                .to(R34Response.class)
                .map(R34Response::getPosts)
                .flatMap(Mono::justOrEmpty)
                .map(R34Posts::getPosts)
                .flatMap(Mono::justOrEmpty)
                .map(RandUtil::randValue);
    }

    private static boolean containsChildren(final R34Post post, final List<String> tags) {
        return post.hasChildren() || tags.stream().anyMatch(tag -> tag.contains("loli") || tag.contains("shota"));
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(final R34Post post, final String tag, final String avatarUrl) {
        return ShadbotUtil.getDefaultEmbed(
                embed -> {
                    post.getSource().ifPresent(source -> {
                        if (NetUtil.isUrl(source)) {
                            embed.setDescription("%n[**Source**](%s)".formatted(source));
                        } else {
                            embed.addField("Source", source, false);
                        }
                    });

                    final String resolution = "%dx%d".formatted(post.getWidth(), post.getHeight());
                    final String formattedTags = Rule34Cmd.formatTags(post.getTags());
                    embed.setAuthor("Rule34: %s".formatted(tag), post.getFileUrl(), avatarUrl)
                            .setThumbnail("https://i.imgur.com/t6JJWFN.png")
                            .addField("Resolution", resolution, false)
                            .addField("Tags", formattedTags, false)
                            .setImage(post.getFileUrl())
                            .setFooter("If there is no preview, click on the title to see the " +
                                    "media (probably a video)", null);
                });
    }

    private static String formatTags(final List<String> tags) {
        final StringBuilder tagsBuilder = new StringBuilder();
        for (final String tag : tags) {
            if (tagsBuilder.length() + tag.length() < MAX_TAGS_CHAR) {
                tagsBuilder.append("`%s` ".formatted(tag));
            } else {
                tagsBuilder.append("...");
                break;
            }
        }
        return tagsBuilder.toString();
    }

}
