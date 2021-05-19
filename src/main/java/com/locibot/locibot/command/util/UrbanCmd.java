package com.locibot.locibot.command.util;

import com.locibot.locibot.api.ServerAccessException;
import com.locibot.locibot.api.json.urbandictionary.UrbanDefinition;
import com.locibot.locibot.api.json.urbandictionary.UrbanDictionaryResponse;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.object.RequestHelper;
import com.locibot.locibot.utils.NetUtil;
import com.locibot.locibot.utils.ShadbotUtil;
import com.locibot.locibot.utils.StringUtil;
import discord4j.core.object.Embed;
import discord4j.core.object.Embed.Field;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.Comparator;
import java.util.function.Consumer;

public class UrbanCmd extends BaseCmd {

    private static final String HOME_URL = "http://api.urbandictionary.com/v0/define";

    public UrbanCmd() {
        super(CommandCategory.UTILS, "urban", "Search for Urban Dictionary definition");
        this.addOption(option -> option.name("word")
                .description("Search for a word")
                .required(true)
                .type(ApplicationCommandOptionType.STRING.getValue()));
    }

    @Override
    public Mono<?> execute(Context context) {
        final String query = context.getOptionAsString("word").orElseThrow();

        return context.isChannelNsfw()
                .flatMap(isNsfw -> {
                    if (!isNsfw) {
                        return context.createFollowupMessage(Emoji.GREY_EXCLAMATION, context.localize("must.be.nsfw"));
                    }

                    return context.createFollowupMessage(Emoji.HOURGLASS, context.localize("urban.loading"))
                            .then(UrbanCmd.getUrbanDefinition(query))
                            .flatMap(urbanDef -> context.editFollowupMessage(UrbanCmd.formatEmbed(context, urbanDef)))
                            .switchIfEmpty(context.editFollowupMessage(Emoji.MAGNIFYING_GLASS,
                                    context.localize("urban.not.found").formatted(query)));
                });
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(Context context, UrbanDefinition urbanDef) {
        final String definition = StringUtil.abbreviate(urbanDef.getDefinition(), Embed.MAX_DESCRIPTION_LENGTH);
        final String example = StringUtil.abbreviate(urbanDef.getExample(), Field.MAX_VALUE_LENGTH);
        return ShadbotUtil.getDefaultEmbed(
                embed -> {
                    embed.setAuthor(context.localize("urban.title").formatted(urbanDef.word()),
                            urbanDef.permalink(), context.getAuthorAvatar())
                            .setThumbnail("https://i.imgur.com/7KJtwWp.png")
                            .setDescription(definition);

                    if (!example.isBlank()) {
                        embed.addField(context.localize("urban.example"), example, false);
                    }
                });
    }

    private static Mono<UrbanDefinition> getUrbanDefinition(String query) {
        final String url = "%s?".formatted(HOME_URL)
                + "term=%s".formatted(NetUtil.encode(query));
        return RequestHelper.fromUrl(url)
                .to(UrbanDictionaryResponse.class)
                .flatMapIterable(UrbanDictionaryResponse::definitions)
                .sort(Comparator.comparingInt(UrbanDefinition::getRatio).reversed())
                .next()
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(ServerAccessException.isStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR))
                        .onRetryExhaustedThrow((spec, signal) -> new IOException("Retries exhausted on error %d"
                                .formatted(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()))));
    }

}
