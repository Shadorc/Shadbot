package com.shadorc.shadbot.command.fun;

import com.shadorc.shadbot.api.json.joke.JokeResponse;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.ShadbotUtil;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

public class JokeCmd extends BaseCmd {

    private static final String HOME_URL = "https://icanhazdadjoke.com/";

    public JokeCmd() {
        super(CommandCategory.FUN, "joke", "Show a random joke");
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.createFollowupMessage(Emoji.HOURGLASS + " (**%s**) Loading joke...", context.getAuthorName())
                .zipWith(JokeCmd.getRandomJoke())
                .flatMap(TupleUtils.function((messageId, joke) ->
                        context.editFollowupMessage(messageId, ShadbotUtil.getDefaultEmbed(
                                embed -> embed.setAuthor("Joke", HOME_URL, context.getAuthorAvatarUrl())
                                        .setDescription(joke)))));
    }

    private static Mono<String> getRandomJoke() {
        return RequestHelper.fromUrl(HOME_URL)
                .addHeaders(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON)
                .to(JokeResponse.class)
                .map(JokeResponse::getJoke);
    }

}
