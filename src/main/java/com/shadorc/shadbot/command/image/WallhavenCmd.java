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
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class WallhavenCmd extends BaseCmd {

    private static final String HOME_URL = "https://wallhaven.cc/api/v1/search";

    public WallhavenCmd() {
        super(CommandCategory.IMAGE, "wallhaven", "Search random wallpaper from Wallhaven");
        this.addOption("query", "Search for a wallpaper", false, ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(final Context context) {
        final String query = context.getOptionAsString("query").orElse("");
        return context.createFollowupMessage(Emoji.HOURGLASS + " (**%s**) Loading wallpaper...", context.getAuthorName())
                .flatMap(messageId -> Mono.zip(WallhavenCmd.getWallpaper(query), context.isChannelNsfw())
                        .flatMap(TupleUtils.function((wallpaper, isNsfw) -> {
                            if (!"sfw".equals(wallpaper.getPurity()) && !isNsfw) {
                                return context.editFollowupMessage(messageId, ShadbotUtil.mustBeNsfw());
                            }

                            final String title = "Wallpaper: %s".formatted(query.isBlank() ? "random" : query);
                            return context.editFollowupMessage(messageId,
                                    WallhavenCmd.formatEmbed(context.getAuthorAvatarUrl(), title, wallpaper));
                        }))
                        .switchIfEmpty(context.editFollowupMessage(messageId,
                                Emoji.MAGNIFYING_GLASS + " (**%s**) No wallpapers found matching query `%s`",
                                context.getAuthorName(), query)));
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(final String avatarUrl, final String title, final Wallpaper wallpaper) {
        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor(title, wallpaper.getUrl(), avatarUrl)
                        .setImage(wallpaper.getPath())
                        .addField("Resolution", wallpaper.getResolution(), false));
    }

    private static Mono<Wallpaper> getWallpaper(final String query) {
        return RequestHelper.fromUrl(WallhavenCmd.buildUrl(query))
                .to(WallhavenResponse.class)
                .map(WallhavenResponse::getWallpapers)
                .filter(Predicate.not(List::isEmpty))
                .map(RandUtil::randValue);
    }

    private static String buildUrl(final String query) {
        final StringBuilder urlBuilder = new StringBuilder(HOME_URL);
        urlBuilder.append(String.format("?apikey=%s",
                CredentialManager.getInstance().get(Credential.WALLHAVEN_API_KEY)));

        if (query.isBlank()) {
            urlBuilder.append("&sorting=toplist")
                    .append("&purity=100");
        } else {
            final String keywords = FormatUtil.format(
                    query.split("[, ]"),
                    keyword -> "+%s".formatted(NetUtil.encode(keyword.trim())),
                    "");
            urlBuilder.append("&q=%s".formatted(keywords))
                    .append("&sorting=relevance");
        }

        return urlBuilder.toString();
    }

}
