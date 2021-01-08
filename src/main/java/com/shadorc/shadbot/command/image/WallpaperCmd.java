/*
package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.json.image.wallhaven.WallhavenResponse;
import com.shadorc.shadbot.api.json.image.wallhaven.Wallpaper;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.NetUtils;
import com.shadorc.shadbot.utils.RandUtils;
import com.shadorc.shadbot.utils.ShadbotUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
                .then(Mono.zip(WallpaperCmd.getWallpaper(arg), context.isChannelNsfw()))
                .map(TupleUtils.function((wallpaper, isNsfw) -> {
                    if (!"sfw".equals(wallpaper.getPurity()) && !isNsfw) {
                        return updatableMsg.setContent(ShadbotUtils.mustBeNsfw(context.getPrefix()));
                    }

                    final String title = String.format("Wallpaper: %s", arg.isBlank() ? "random" : arg);
                    return updatableMsg.setEmbed(ShadbotUtils.getDefaultEmbed()
                            .andThen(embed -> embed.setAuthor(title, wallpaper.getUrl(), context.getAvatarUrl())
                                    .setImage(wallpaper.getPath())
                                    .addField("Resolution", wallpaper.getResolution(), false)));
                }))
                .switchIfEmpty(Mono.fromCallable(() -> updatableMsg.setContent(
                        String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) No wallpapers were found for the search `%s`",
                                context.getUsername(), arg))))
                .flatMap(UpdatableMessage::send)
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
                .then();
    }

    private static Mono<Wallpaper> getWallpaper(String arg) {
        return RequestHelper.fromUrl(WallpaperCmd.buildUrl(arg))
                .to(WallhavenResponse.class)
                .map(WallhavenResponse::getWallpapers)
                .filter(Predicate.not(List::isEmpty))
                .map(RandUtils::randValue);
    }

    private static String buildUrl(String arg) {
        final StringBuilder urlBuilder = new StringBuilder(HOME_URL);
        urlBuilder.append(String.format("?apikey=%s",
                CredentialManager.getInstance().get(Credential.WALLHAVEN_API_KEY)));

        if (arg.isBlank()) {
            urlBuilder.append("&sorting=toplist");
            urlBuilder.append("&purity=100");
        } else {
            final String keywords = FormatUtils.format(
                    arg.split("[, ]"),
                    keyword -> String.format("+%s", NetUtils.encode(keyword.trim())),
                    "");
            urlBuilder.append(String.format("&q=%s", keywords));
            urlBuilder.append("&sorting=relevance");
        }

        return urlBuilder.toString();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Search for a wallpaper.")
                .addArg("search", "keywords (e.g. doom game)", true)
                .setSource("https://wallhaven.cc/")
                .build();
    }
}*/
