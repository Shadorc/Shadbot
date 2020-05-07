package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.json.image.wallhaven.WallhavenResponse;
import com.shadorc.shadbot.api.json.image.wallhaven.Wallpaper;
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
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class WallpaperCmd extends BaseCmd {

    private static final String HOME_URL = "https://wallhaven.cc/api/v1/search";

    public WallpaperCmd() {
        super(CommandCategory.IMAGE, List.of("wallpaper"), "wp");
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.getArg().orElse("");
        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());
        return updatableMsg.setContent(String.format(Emoji.HOURGLASS + " (**%s**) Loading wallpaper...", context.getUsername()))
                .send()
                .thenReturn(new StringBuilder(HOME_URL))
                .map(urlBuilder -> {
                    urlBuilder.append(String.format("?apikey=%s", CredentialManager.getInstance().get(Credential.WALLHAVEN_API_KEY)));

                    if (arg.isBlank()) {
                        urlBuilder.append("&sorting=toplist");
                    } else {
                        final String keywords = Arrays.stream(arg.split("[, ]"))
                                .map(keyword -> String.format("+%s", NetUtils.encode(keyword.trim())))
                                .collect(Collectors.joining());
                        urlBuilder.append(String.format("&q=%s", keywords));
                        urlBuilder.append("&sorting=relevance");
                    }

                    return urlBuilder.toString();
                })
                .flatMap(url -> NetUtils.get(url, WallhavenResponse.class))
                .map(WallhavenResponse::getWallpapers)
                .map(wallpapers -> {
                    if (wallpapers.isEmpty()) {
                        return updatableMsg.setContent(
                                String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) No wallpapers were found for the search `%s`",
                                        context.getUsername(), arg));
                    }

                    final Wallpaper wallpaper = Utils.randValue(wallpapers);
                    return updatableMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                            .andThen(embed -> embed.setAuthor("Wallpaper", wallpaper.getUrl(), context.getAvatarUrl())
                                    .setImage(wallpaper.getPath())
                                    .addField("Resolution", wallpaper.getResolution(), false)));
                })
                .flatMap(UpdatableMessage::send)
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Search for a wallpaper.")
                .addArg("search", "keywords (e.g. doom game)", true)
                .setSource("https://wallhaven.cc/")
                .build();
    }
}