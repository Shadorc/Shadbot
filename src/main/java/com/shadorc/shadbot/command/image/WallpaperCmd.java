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
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.NetUtil;
import com.shadorc.shadbot.utils.RandUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.List;
import java.util.function.Predicate;

public class WallpaperCmd extends BaseCmd {

    private static final String HOME_URL = "https://wallhaven.cc/api/v1/search";

    public WallpaperCmd() {
        super(CommandCategory.IMAGE, "wallpaper", "Search for a wallpaper");
        this.addOption("search",
                "Keywords to search (e.g. doom game)",
                false,
                ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        final String search = context.getOption("search").orElse("");
        return context.createFollowupMessage(Emoji.HOURGLASS + " (**%s**) Loading wallpaper...", context.getAuthorName())
                .flatMap(messageId -> Mono.zip(WallpaperCmd.getWallpaper(search), context.isChannelNsfw())
                        .flatMap(TupleUtils.function((wallpaper, isNsfw) -> {
                            if (!"sfw".equals(wallpaper.getPurity()) && !isNsfw) {
                                return context.editFollowupMessage(messageId, ShadbotUtil.mustBeNsfw());
                            }

                            final String title = String.format("Wallpaper: %s", search.isBlank() ? "random" : search);
                            return context.editFollowupMessage(messageId, ShadbotUtil.getDefaultEmbed(
                                    embed -> embed.setAuthor(title, wallpaper.getUrl(), context.getAuthorAvatarUrl())
                                            .setImage(wallpaper.getPath())
                                            .addField("Resolution", wallpaper.getResolution(), false)));
                        }))
                        .switchIfEmpty(context.editFollowupMessage(messageId,
                                Emoji.MAGNIFYING_GLASS + " (**%s**) No wallpapers were found for the search `%s`",
                                context.getAuthorName(), search)));
    }

    private static Mono<Wallpaper> getWallpaper(String arg) {
        return RequestHelper.fromUrl(WallpaperCmd.buildUrl(arg))
                .to(WallhavenResponse.class)
                .map(WallhavenResponse::getWallpapers)
                .filter(Predicate.not(List::isEmpty))
                .map(RandUtil::randValue);
    }

    private static String buildUrl(String arg) {
        final StringBuilder urlBuilder = new StringBuilder(HOME_URL);
        urlBuilder.append(String.format("?apikey=%s",
                CredentialManager.getInstance().get(Credential.WALLHAVEN_API_KEY)));

        if (arg.isBlank()) {
            urlBuilder.append("&sorting=toplist");
            urlBuilder.append("&purity=100");
        } else {
            final String keywords = FormatUtil.format(
                    arg.split("[, ]"),
                    keyword -> String.format("+%s", NetUtil.encode(keyword.trim())),
                    "");
            urlBuilder.append(String.format("&q=%s", keywords));
            urlBuilder.append("&sorting=relevance");
        }

        return urlBuilder.toString();
    }

}
