package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.json.image.giphy.GiphyGif;
import com.shadorc.shadbot.api.json.image.giphy.GiphyResponse;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NetUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Flux;
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
        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());

        return updatableMsg.setContent(String.format(Emoji.HOURGLASS + " (**%s**) Loading gif...", context.getUsername()))
                .send()
                .then(this.getGif(NetUtils.encode(context.getArg().orElse(""))))
                .map(gifUrl -> updatableMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                        .andThen(embed -> embed.setImage(gifUrl))))
                .switchIfEmpty(Mono.defer(() -> Mono.just(updatableMsg.setContent(
                        String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) No gifs were found for the search `%s`",
                                context.getUsername(), context.getArg().orElse("random search"))))))
                .flatMap(UpdatableMessage::send)
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
                .then();
    }

    private Mono<String> getGif(String search) {
        final String url = String.format("%s?api_key=%s&tag=%s",
                HOME_URl, CredentialManager.getInstance().get(Credential.GIPHY_API_KEY), NetUtils.encode(search));

        return NetUtils.get(url, GiphyResponse.class)
                .map(GiphyResponse::getGifs)
                .flatMapMany(Flux::fromIterable)
                .next()
                .map(GiphyGif::getImageUrl);
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Show a random gif.")
                .addArg("tag", "the tag to search", true)
                .setSource("https://www.giphy.com/")
                .build();
    }

}
