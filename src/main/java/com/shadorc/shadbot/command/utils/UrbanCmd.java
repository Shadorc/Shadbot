package com.shadorc.shadbot.command.utils;

import com.shadorc.shadbot.api.ServerAccessException;
import com.shadorc.shadbot.api.json.urbandictionary.UrbanDefinition;
import com.shadorc.shadbot.api.json.urbandictionary.UrbanDictionaryResponse;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.NetUtils;
import com.shadorc.shadbot.utils.ShadbotUtils;
import com.shadorc.shadbot.utils.StringUtils;
import discord4j.core.object.Embed;
import discord4j.core.object.Embed.Field;
import discord4j.core.spec.EmbedCreateSpec;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

public class UrbanCmd extends BaseCmd {

    private static final String HOME_URL = "http://api.urbandictionary.com/v0/define";

    public UrbanCmd() {
        super(CommandCategory.UTILS, List.of("urban"), "ud");
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());

        return updatableMsg.setContent(String.format(Emoji.HOURGLASS + " (**%s**) Loading Urban Dictionary definition...",
                context.getUsername()))
                .send()
                .then(context.isChannelNsfw())
                .flatMap(isNsfw -> {
                    if (!isNsfw) {
                        return Mono.just(updatableMsg.setContent(ShadbotUtils.mustBeNsfw(context.getPrefix())));
                    }

                    return UrbanCmd.getUrbanDefinition(arg)
                            .map(urbanDefinition -> {
                                final String definition = StringUtils.abbreviate(urbanDefinition.getDefinition(),
                                        Embed.MAX_DESCRIPTION_LENGTH);
                                final String example = StringUtils.abbreviate(urbanDefinition.getExample(), Field.MAX_VALUE_LENGTH);

                                return updatableMsg.setEmbed(ShadbotUtils.getDefaultEmbed()
                                        .andThen(embed -> {
                                            embed.setAuthor(String.format("Urban Dictionary: %s",
                                                    urbanDefinition.getWord()), urbanDefinition.getPermalink(), context.getAvatarUrl())
                                                    .setThumbnail("https://i.imgur.com/7KJtwWp.png")
                                                    .setDescription(definition);

                                            if (!example.isBlank()) {
                                                embed.addField("Example", example, false);
                                            }
                                        }));
                            });
                })
                .switchIfEmpty(Mono.fromCallable(() -> updatableMsg.setContent(
                        String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) No Urban Dictionary definition found for `%s`",
                                context.getUsername(), arg))))
                .flatMap(UpdatableMessage::send)
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
                .then();
    }

    private static Mono<UrbanDefinition> getUrbanDefinition(String search) {
        final String url = String.format("%s?term=%s", HOME_URL, NetUtils.encode(search));
        return RequestHelper.fromUrl(url)
                .to(UrbanDictionaryResponse.class)
                .flatMapIterable(UrbanDictionaryResponse::getDefinitions)
                .next()
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(ServerAccessException.isStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)));
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Show the first Urban Dictionary definition for a search.")
                .addArg("search", false)
                .build();
    }

}
