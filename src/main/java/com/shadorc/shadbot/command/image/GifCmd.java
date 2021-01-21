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
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.NetUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class GifCmd extends BaseCmd {

    private static final String API_URL = "https://api.giphy.com/v1/gifs";
    private static final String RENDOM_ENDPOINT = String.format("%s/random", API_URL);
    private static final String SEARCH_ENDPOINT = String.format("%s/search", API_URL);

    public GifCmd() {
        super(CommandCategory.IMAGE, "gif", "Search for a random gif");
        this.setDefaultRateLimiter();
    }

    @Override
    public ApplicationCommandRequest build(ImmutableApplicationCommandRequest.Builder builder) {
        return builder
                .addOption(ApplicationCommandOptionData.builder()
                        .name("keyword")
                        .description("The keyword to search")
                        .type(ApplicationCommandOptionType.STRING.getValue())
                        .required(false)
                        .build())
                .build();
    }

    @Override
    public Mono<?> execute(Context context) {
        final Optional<String> keyword = context.getOption("tag");
        return context.createFollowupMessage(Emoji.HOURGLASS + " (**%s**) Loading gif...", context.getAuthorName())
                .flatMap(messageId -> GifCmd.getGifUrl(keyword.orElse(""))
                        .flatMap(gifUrl -> context.editFollowupMessage(messageId,
                                ShadbotUtil.getDefaultEmbed(spec -> spec.setImage(gifUrl))))
                        .switchIfEmpty(context.editFollowupMessage(messageId,
                                Emoji.MAGNIFYING_GLASS + " (**%s**) No gifs were found for the search `%s`",
                                context.getAuthorName(), keyword.orElse("random search"))));
    }

    private static Mono<String> getGifUrl(String keyword) {
        final String apiKey = CredentialManager.getInstance().get(Credential.GIPHY_API_KEY);
        final String encodedSearch = Objects.requireNonNull(NetUtil.encode(keyword));

        final String url;
        if (encodedSearch.isBlank()) {
            url = String.format("%s?api_key=%s",
                    RENDOM_ENDPOINT, apiKey);
        } else {
            url = String.format("%s?api_key=%s&q=%s&limit=1&offset=%d",
                    SEARCH_ENDPOINT, apiKey, encodedSearch, ThreadLocalRandom.current().nextInt(25));
        }

        return RequestHelper.fromUrl(url)
                .to(GiphyResponse.class)
                .flatMapIterable(GiphyResponse::getData)
                .next()
                .map(Data::getImages)
                .map(Images::getOriginal)
                .map(Original::getUrl);
    }

}
