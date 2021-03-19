/*
package com.shadorc.shadbot.command.util;

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
import discord4j.rest.util.ApplicationCommandOptionType;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.function.Consumer;

public class UrbanCmd extends BaseCmd {

    private static final String HOME_URL = "http://api.urbandictionary.com/v0/define";

    public UrbanCmd() {
        super(CommandCategory.UTILS, "urban", "Search for Urban Dictionary definition");
        this.addOption("word", "Search for a word", true, ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        final String query = context.getOptionAsString("word").orElseThrow();

        return context.isChannelNsfw()
                .flatMap(isNsfw -> {
                    if (!isNsfw) {
                        return context.createFollowupMessage(ShadbotUtil.mustBeNsfw());
                    }

                    return context.createFollowupMessage(
                            Emoji.HOURGLASS + " (**%s**) Loading Urban Dictionary definition...", context.getAuthorName())
                            .flatMap(messageId -> UrbanCmd.getUrbanDefinition(query)
                                    .flatMap(urbanDef -> context.editFollowupMessage(messageId,
                                            UrbanCmd.formatEmbed(urbanDef, context.getAuthorAvatar())))
                                    .switchIfEmpty(context.editFollowupMessage(messageId,
                                            Emoji.MAGNIFYING_GLASS + " (**%s**) No definition matching word `%s`",
                                            context.getAuthorName(), query)));
                });
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(final UrbanDefinition urbanDef, final String avatarUrl) {
        final String definition = StringUtil.abbreviate(urbanDef.getDefinition(), Embed.MAX_DESCRIPTION_LENGTH);
        final String example = StringUtil.abbreviate(urbanDef.getExample(), Field.MAX_VALUE_LENGTH);
        return ShadbotUtil.getDefaultEmbed(
                embed -> {
                    embed.setAuthor("Urban Dictionary: %s".formatted(urbanDef.getWord()), urbanDef.getPermalink(), avatarUrl)
                            .setThumbnail("https://i.imgur.com/7KJtwWp.png")
                            .setDescription(definition);

                    if (!example.isBlank()) {
                        embed.addField("Example", example, false);
                    }
                });
    }

    private static Mono<UrbanDefinition> getUrbanDefinition(String query) {
        final String url = String.format("%s?term=%s", HOME_URL, NetUtil.encode(query));
        return RequestHelper.fromUrl(url)
                .to(UrbanDictionaryResponse.class)
                .flatMapIterable(UrbanDictionaryResponse::getDefinitions)
                .next()
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(ServerAccessException.isStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)));
    }

}
*/
