package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.image.giphy.GiphyResponse;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.Credentials;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.object.message.LoadingMessage;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NetUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class GifCmd extends BaseCmd {

    private static final String HOME_URl = "https://api.giphy.com/v1/gifs/random";

    public GifCmd() {
        super(CommandCategory.IMAGE, List.of("gif"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());
        final String url = String.format("%s?api_key=%s&tag=%s",
                HOME_URl, Credentials.get(Credential.GIPHY_API_KEY), NetUtils.encode(context.getArg().orElse("")));

        return NetUtils.get(url, GiphyResponse.class)
                .map(giphy -> {
                    if (giphy.getGifs().isEmpty()) {
                        return loadingMsg.setContent(String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) No gifs were found for the search `%s`",
                                context.getUsername(), context.getArg().orElse("random search")));
                    }

                    return loadingMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                            .andThen(embed -> embed.setImage(giphy.getGifs().get(0).getImageUrl())));
                })
                .flatMap(LoadingMessage::send)
                .doOnTerminate(loadingMsg::stopTyping)
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show a random gif")
                .addArg("tag", "the tag to search", true)
                .setSource("https://www.giphy.com/")
                .build();
    }

}
