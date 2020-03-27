package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.json.image.r34.R34Post;
import com.shadorc.shadbot.api.json.image.r34.R34Posts;
import com.shadorc.shadbot.api.json.image.r34.R34Response;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.*;
import discord4j.core.spec.EmbedCreateSpec;
import org.json.JSONObject;
import org.json.XML;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class Rule34Cmd extends BaseCmd {

    private static final String HOME_URL = "https://rule34.xxx/index.php";
    private static final int MAX_TAGS_CHAR = 250;

    public Rule34Cmd() {
        super(CommandCategory.IMAGE, List.of("rule34"), "r34");
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();
        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());

        return updatableMsg.setContent(String.format(Emoji.HOURGLASS + " (**%s**) Loading rule34 image...", context.getUsername()))
                .send()
                .then(context.isChannelNsfw())
                .flatMap(isNsfw -> {
                    if (!isNsfw) {
                        return Mono.just(updatableMsg.setContent(TextUtils.mustBeNsfw(context.getPrefix())));
                    }

                    return this.getR34Post(arg)
                            .map(post -> {
                                final List<String> tags = StringUtils.split(post.getTags(), " ");

                                // Don't post images containing children
                                if (post.hasChildren() || tags.stream().anyMatch(tag -> tag.contains("loli") || tag.contains("shota"))) {
                                    return updatableMsg.setContent(
                                            String.format(Emoji.WARNING + " (**%s**) I don't display images " +
                                                            "containing children or tagged with `loli` or `shota`.",
                                                    context.getUsername()));
                                }

                                return updatableMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                                        .andThen(embed -> {
                                            if (!post.getSource().isEmpty()) {
                                                if (post.getSource().startsWith("http")) {
                                                    embed.setDescription(String.format("%n[**Source**](%s)", post.getSource()));
                                                } else {
                                                    embed.addField("Source", post.getSource(), false);
                                                }
                                            }

                                            embed.setAuthor(String.format("Rule34: %s", arg), post.getFileUrl(), context.getAvatarUrl())
                                                    .setThumbnail("https://i.imgur.com/t6JJWFN.png")
                                                    .addField("Resolution", String.format("%dx%s",
                                                            post.getWidth(), post.getHeight()), false)
                                                    .addField("Tags", this.formatTags(tags), false)
                                                    .setImage(post.getFileUrl())
                                                    .setFooter("If there is no preview, click on the title to " +
                                                            "see the media (probably a video)", null);
                                        }));
                            });
                })
                .switchIfEmpty(Mono.defer(() -> Mono.just(updatableMsg.setContent(
                        String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) No images were found for the search `%s`",
                                context.getUsername(), arg)))))
                .flatMap(UpdatableMessage::send)
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
                .then();
    }

    private Mono<R34Post> getR34Post(String search) {
        final String url = String.format("%s?page=dapi&s=post&q=index&tags=%s",
                HOME_URL, NetUtils.encode(search.replace(" ", "_")));

        return NetUtils.get(url)
                .map(XML::toJSONObject)
                .map(JSONObject::toString)
                .flatMap(value -> Mono.fromCallable(() -> Utils.MAPPER.readValue(value, R34Response.class)))
                .map(R34Response::getPosts)
                .flatMap(Mono::justOrEmpty)
                .map(R34Posts::getPosts)
                .flatMap(Mono::justOrEmpty)
                .map(Utils::randValue);
    }

    private String formatTags(List<String> tags) {
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

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Show a random image corresponding to a tag from Rule34 website.")
                .setSource("https://www.rule34.xxx/")
                .addArg("tag", false)
                .build();
    }
}
