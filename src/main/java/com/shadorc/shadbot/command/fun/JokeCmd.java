package com.shadorc.shadbot.command.fun;

import com.shadorc.shadbot.api.joke.JokeResponse;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NetUtils;
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
                .then(NetUtils.get(header -> header.add(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON), HOME_URL, JokeResponse.class))
                .map(response -> updatableMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                        .andThen(embed -> embed.setAuthor("Joke", HOME_URL, context.getAvatarUrl())
                                .setDescription(response.getJoke()))))
                .flatMap(UpdatableMessage::send)
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show a random joke.")
                .setSource(HOME_URL)
                .build();
    }
}
