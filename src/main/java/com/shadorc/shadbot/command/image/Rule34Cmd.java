package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.json.image.r34.R34Post;
import com.shadorc.shadbot.api.json.image.r34.R34Posts;
import com.shadorc.shadbot.api.json.image.r34.R34Response;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.NetUtils;
import com.shadorc.shadbot.utils.RandUtils;
import com.shadorc.shadbot.utils.ShadbotUtils;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class Rule34Cmd extends BaseCmd {

    private static final String HOME_URL = "https://rule34.xxx/index.php";
    private static final int MAX_TAGS_CHAR = 250;

    public Rule34Cmd() {
        super(CommandCategory.IMAGE, "rule34", "Show a random image corresponding to a tag from Rule34 website");
        this.setDefaultRateLimiter();
    }

    @Override
    public ApplicationCommandRequest build(ImmutableApplicationCommandRequest.Builder builder) {
        return builder
                .addOption(ApplicationCommandOptionData.builder()
                        .name("tag")
                        .description("The tag to search")
                        .type(ApplicationCommandOptionType.STRING.getValue())
                        .required(true)
                        .build())
                .build();
    }

    @Override
    public Mono<?> execute(Context context) {
        final String tag = context.getOption("tag").orElseThrow();

        return context.isChannelNsfw()
                .flatMap(isNsfw -> {
                    if (!isNsfw) {
                        return context.createFollowupMessage(ShadbotUtils.mustBeNsfw());
                    }

                    return context.createFollowupMessage(Emoji.HOURGLASS + " (**%s**) Loading rule34 image...", context.getAuthorName())
                            .flatMap(messageId -> Rule34Cmd.getR34Post(tag)
                                    .flatMap(post -> {
                                        // Don't post images containing children
                                        if (Rule34Cmd.containsChildren(post, post.getTags())) {
                                            return context.editFollowupMessage(messageId,
                                                    Emoji.WARNING + " (**%s**) I don't display images " +
                                                            "containing children or tagged with `loli` or `shota`.",
                                                    context.getAuthorName());
                                        }

                                        return context.editFollowupMessage(messageId,
                                                Rule34Cmd.formatEmbed(post, tag, context.getAuthorAvatarUrl()));
                                    })
                                    .switchIfEmpty(context.editFollowupMessage(messageId,
                                            Emoji.MAGNIFYING_GLASS + " (**%s**) No images were found for the search `%s`",
                                            context.getAuthorName(), tag)));
                });
    }

    private static Mono<R34Post> getR34Post(String search) {
        final String url = String.format("%s?page=dapi&s=post&q=index&tags=%s",
                HOME_URL, NetUtils.encode(search.replace(" ", "_")));

        return RequestHelper.fromUrl(url)
                .to(R34Response.class)
                .map(R34Response::getPosts)
                .flatMap(Mono::justOrEmpty)
                .map(R34Posts::getPosts)
                .flatMap(Mono::justOrEmpty)
                .map(RandUtils::randValue);
    }

    private static boolean containsChildren(R34Post post, List<String> tags) {
        return post.hasChildren() || tags.stream().anyMatch(tag -> tag.contains("loli") || tag.contains("shota"));
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(final R34Post post, final String tag, final String avatarUrl) {
        return ShadbotUtils.getDefaultEmbed(
                embed -> {
                    post.getSource().ifPresent(source -> {
                        if (NetUtils.isUrl(source)) {
                            embed.setDescription(String.format("%n[**Source**](%s)", source));
                        } else {
                            embed.addField("Source", source, false);
                        }
                    });

                    final String resolution = String.format("%dx%d", post.getWidth(), post.getHeight());
                    final String formattedTags = Rule34Cmd.formatTags(post.getTags());
                    embed.setAuthor(String.format("Rule34: %s", tag), post.getFileUrl(), avatarUrl)
                            .setThumbnail("https://i.imgur.com/t6JJWFN.png")
                            .addField("Resolution", resolution, false)
                            .addField("Tags", formattedTags, false)
                            .setImage(post.getFileUrl())
                            .setFooter("If there is no preview, click on the title to " +
                                    "see the media (probably a video)", null);
                });
    }

    private static String formatTags(List<String> tags) {
        final StringBuilder tagsBuilder = new StringBuilder();
        for (final String tag : tags) {
            if (tagsBuilder.length() + tag.length() < MAX_TAGS_CHAR) {
                tagsBuilder.append(String.format("`%s` ", tag));
            } else {
                tagsBuilder.append("...");
                break;
            }
        }
        return tagsBuilder.toString();
    }

}
