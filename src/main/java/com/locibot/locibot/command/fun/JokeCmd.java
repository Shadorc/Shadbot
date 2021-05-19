package com.locibot.locibot.command.fun;

import com.locibot.locibot.api.json.joke.JokeResponse;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.object.RequestHelper;
import com.locibot.locibot.utils.ShadbotUtil;
import discord4j.core.spec.EmbedCreateSpec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public class JokeCmd extends BaseCmd {

    private static final String HOME_URL = "https://icanhazdadjoke.com/";

    public JokeCmd() {
        super(CommandCategory.FUN, "joke", "Random dad joke");
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.createFollowupMessage(Emoji.HOURGLASS, context.localize("joke.loading"))
                .then(JokeCmd.getRandomJoke())
                .flatMap(joke -> context.editFollowupMessage(JokeCmd.formatEmbed(context, joke)));
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(Context context, String joke) {
        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor(context.localize("joke.title"), HOME_URL, context.getAuthorAvatar())
                        .setDescription(joke));
    }

    private static Mono<String> getRandomJoke() {
        return RequestHelper.fromUrl(HOME_URL)
                .addHeaders(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON)
                .to(JokeResponse.class)
                .map(JokeResponse::joke);
    }

}
