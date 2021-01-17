package com.shadorc.shadbot.command.utils;

import com.shadorc.shadbot.api.ServerAccessException;
import com.shadorc.shadbot.api.json.urbandictionary.UrbanDefinition;
import com.shadorc.shadbot.api.json.urbandictionary.UrbanDictionaryResponse;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.NetUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import com.shadorc.shadbot.utils.StringUtil;
import discord4j.core.object.Embed;
import discord4j.core.object.Embed.Field;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.function.Consumer;

public class UrbanCmd extends BaseCmd {

    private static final String HOME_URL = "http://api.urbandictionary.com/v0/define";

    public UrbanCmd() {
        super(CommandCategory.UTILS, "urban", "Show the first Urban Dictionary definition for a search");
        this.setDefaultRateLimiter();
    }

    @Override
    public ApplicationCommandRequest build(ImmutableApplicationCommandRequest.Builder builder) {
        return builder
                .addOption(ApplicationCommandOptionData.builder()
                        .name("search")
                        .description("The word to search")
                        .type(ApplicationCommandOptionType.STRING.getValue())
                        .required(true)
                        .build())
                .build();
    }

    @Override
    public Mono<?> execute(Context context) {
        final String search = context.getOption("search").orElseThrow();

        return context.isChannelNsfw()
                .flatMap(isNsfw -> {
                    if (!isNsfw) {
                        return context.createFollowupMessage(ShadbotUtil.mustBeNsfw());
                    }

                    return context.createFollowupMessage(
                            Emoji.HOURGLASS + " (**%s**) Loading Urban Dictionary definition...", context.getAuthorName())
                            .flatMap(messageId -> UrbanCmd.getUrbanDefinition(search)
                                    .flatMap(urbanDef -> context.editFollowupMessage(messageId,
                                            UrbanCmd.formatEmbed(urbanDef, context.getAuthorAvatarUrl())))
                                    .switchIfEmpty(context.editFollowupMessage(messageId,
                                            Emoji.MAGNIFYING_GLASS + " (**%s**) No Urban Dictionary definition found for `%s`",
                                            context.getAuthorName(), search)));
                });
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(final UrbanDefinition urbanDef, final String avatarUrl) {
        final String definition = StringUtil.abbreviate(urbanDef.getDefinition(), Embed.MAX_DESCRIPTION_LENGTH);
        final String example = StringUtil.abbreviate(urbanDef.getExample(), Field.MAX_VALUE_LENGTH);
        return ShadbotUtil.getDefaultEmbed(
                embed -> {
                    embed.setAuthor(String.format("Urban Dictionary: %s",
                            urbanDef.getWord()), urbanDef.getPermalink(), avatarUrl)
                            .setThumbnail("https://i.imgur.com/7KJtwWp.png")
                            .setDescription(definition);

                    if (!example.isBlank()) {
                        embed.addField("Example", example, false);
                    }
                });
    }

    private static Mono<UrbanDefinition> getUrbanDefinition(String search) {
        final String url = String.format("%s?term=%s", HOME_URL, NetUtil.encode(search));
        return RequestHelper.fromUrl(url)
                .to(UrbanDictionaryResponse.class)
                .flatMapIterable(UrbanDictionaryResponse::getDefinitions)
                .next()
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(ServerAccessException.isStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)));
    }

}
