package com.shadorc.shadbot.command.fun;

import com.shadorc.shadbot.api.json.joke.JokeResponse;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.ShadbotUtils;
import discord4j.core.spec.EmbedCreateSpec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class JokeCmd extends BaseCmd {

    private static final String HOME_URL = "https://icanhazdadjoke.com/";

    public JokeCmd() {
        super(CommandCategory.FUN, List.of("joke"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());
        return updatableMsg.setContent(String.format(Emoji.HOURGLASS + " (**%s**) Loading joke...", context.getUsername()))
                .send()
                .then(JokeCmd.getRandomJoke())
                .map(joke -> updatableMsg.setEmbed(ShadbotUtils.getDefaultEmbed()
                        .andThen(embed -> embed.setAuthor("Joke", HOME_URL, context.getAvatarUrl())
                                .setDescription(joke))))
                .flatMap(UpdatableMessage::send)
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
                .then();
    }

    private static Mono<String> getRandomJoke() {
        return RequestHelper.fromUrl(HOME_URL)
                .addHeaders(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON)
                .to(JokeResponse.class)
                .map(JokeResponse::getJoke);
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Show a random joke.")
                .setSource(HOME_URL)
                .build();
    }
}
