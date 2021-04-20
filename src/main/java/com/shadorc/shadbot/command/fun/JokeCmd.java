package com.shadorc.shadbot.command.fun;

import com.shadorc.shadbot.api.json.joke.JokeResponse;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.ShadbotUtil;
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
        return context.reply(Emoji.HOURGLASS, context.localize("joke.loading"))
                .then(JokeCmd.getRandomJoke())
                .flatMap(joke -> context.editReply(JokeCmd.formatEmbed(context, joke)));
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
