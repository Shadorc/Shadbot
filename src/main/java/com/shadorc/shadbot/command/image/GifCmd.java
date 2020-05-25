package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.json.image.giphy.Data;
import com.shadorc.shadbot.api.json.image.giphy.GiphyResponse;
import com.shadorc.shadbot.api.json.image.giphy.Images;
import com.shadorc.shadbot.api.json.image.giphy.Original;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NetUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class GifCmd extends BaseCmd {

    private static final String RENDOM_ENDPOINT = "https://api.giphy.com/v1/gifs/random";
    private static final String SEARCH_ENDPOINT = "https://api.giphy.com/v1/gifs/search";

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
                .switchIfEmpty(Mono.fromCallable(() -> updatableMsg.setContent(
                        String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) No gifs were found for the search `%s`",
                                context.getUsername(), context.getArg().orElse("random search")))))
                .flatMap(UpdatableMessage::send)
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
                .then();
    }

    private Mono<String> getGif(String encodedSearch) {
        final String apiKey = CredentialManager.getInstance().get(Credential.GIPHY_API_KEY);
        final String url;
        if (encodedSearch.isBlank()) {
            url = String.format("%s?api_key=%s", RENDOM_ENDPOINT, apiKey);
        } else {
            url = String.format("%s?api_key=%s&q=%s&limit=1&offset=%d",
                    SEARCH_ENDPOINT, apiKey, encodedSearch, ThreadLocalRandom.current().nextInt(25));
        }

        return NetUtils.get(url, GiphyResponse.class)
                .map(GiphyResponse::getData)
                .flatMapMany(Flux::fromIterable)
                .next()
                .map(Data::getImages)
                .map(Images::getOriginal)
                .map(Original::getUrl);
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Show a random gif.")
                .addArg("tag", "the tag to search", true)
                .setSource("https://www.giphy.com/")
                .build();
    }

}
