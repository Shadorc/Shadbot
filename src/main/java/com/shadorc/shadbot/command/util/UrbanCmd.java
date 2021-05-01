package com.shadorc.shadbot.command.util;

import com.shadorc.shadbot.api.ServerAccessException;
import com.shadorc.shadbot.api.json.urbandictionary.UrbanDefinition;
import com.shadorc.shadbot.api.json.urbandictionary.UrbanDictionaryResponse;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.command.Setting;
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
                        return context.reply(Emoji.GREY_EXCLAMATION,
                                context.localize("must.be.nsfw").formatted(Setting.NSFW));
                    }

                    return context.reply(Emoji.HOURGLASS, context.localize("urban.loading"))
                            .then(UrbanCmd.getUrbanDefinition(query))
                            .flatMap(urbanDef -> context.editReply(UrbanCmd.formatEmbed(context, urbanDef)))
                            .switchIfEmpty(context.editReply(Emoji.MAGNIFYING_GLASS,
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
