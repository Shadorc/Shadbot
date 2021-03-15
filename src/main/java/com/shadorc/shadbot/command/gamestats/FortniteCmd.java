package com.shadorc.shadbot.command.gamestats;

import com.shadorc.shadbot.api.json.gamestats.fortnite.FortniteResponse;
import com.shadorc.shadbot.api.json.gamestats.fortnite.Stats;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.NetUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.function.Consumer;

public class FortniteCmd extends BaseCmd {

    private enum Platform {
        PC, XBL, PSN
    }

    private final String apiKey;

    public FortniteCmd() {
        super(CommandCategory.GAMESTATS, "fortnite", "Search for Fortnite statistics");
        this.addOption("platform", "User's platform", true, ApplicationCommandOptionType.STRING,
                DiscordUtil.toOptions(Platform.class));
        this.addOption("username", "Epic nickname", true, ApplicationCommandOptionType.STRING);

        this.apiKey = CredentialManager.getInstance().get(Credential.FORTNITE_API_KEY);
    }

    @Override
    public Mono<?> execute(Context context) {
        final Platform platform = context.getOptionAsEnum(Platform.class, "platform").orElseThrow();
        final String username = context.getOptionAsString("username").orElseThrow();

        final String encodedUsername = NetUtil.encode(username.replace(" ", "%20"));
        final String url = FortniteCmd.buildApiUrl(platform, encodedUsername);

        return context.createFollowupMessage(Emoji.HOURGLASS + " (**%s**) Loading Fortnite stats...", context.getAuthorName())
                .flatMap(messageId -> RequestHelper.fromUrl(url)
                        .addHeaders("TRN-Api-Key", this.apiKey)
                        .to(FortniteResponse.class)
                        .flatMap(fortnite -> {
                            if ("Player Not Found".equals(fortnite.getError().orElse(""))) {
                                throw Exceptions.propagate(new IOException("HTTP Error 400. The request URL is invalid."));
                            }

                            final String profileUrl = FortniteCmd.buildProfileUrl(platform, encodedUsername);
                            final String desc = FortniteCmd.formatDescription(fortnite.getStats(), username);
                            return context.editReply(messageId,
                                    FortniteCmd.formatEmbed(context.getAuthorAvatar(), profileUrl, desc));
                        })
                        .onErrorResume(FortniteCmd::isNotFound,
                                err -> context.editReply(messageId,
                                        String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) This user doesn't play Fortnite " +
                                                        "on this platform or doesn't exist. Please make sure your spelling is" +
                                                        " correct, or follow this guide if you play on Console: " +
                                                        "<https://fortnitetracker.com/profile/search>",
                                                context.getAuthorName()))));
    }

    private static boolean isNotFound(final Throwable err) {
        return err.getMessage().contains("HTTP Error 400. The request URL is invalid.")
                || err.getMessage().contains("wrong header");
    }

    private static String buildApiUrl(final Platform platform, final String encodedUsername) {
        return "https://api.fortnitetracker.com/v1/profile/%s/%s"
                .formatted(platform.name().toLowerCase(), encodedUsername);
    }

    private static String buildProfileUrl(final Platform platform, final String encodedUsername) {
        return "https://fortnitetracker.com/profile/%s/%s"
                .formatted(platform.name().toLowerCase(), encodedUsername);
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(final String avatarUrl, final String profileUrl, final String desc) {
        return ShadbotUtil.getDefaultEmbed(embed ->
                embed.setAuthor("Fortnite Stats", profileUrl, avatarUrl)
                        .setThumbnail("https://i.imgur.com/8NrvS8e.png")
                        .setDescription(desc));
    }

    private static String formatDescription(final Stats stats, final String username) {
        final int length = 8;
        final String format = "%n%-" + (length + 5) + "s %-" + length + "s %-" + length + "s %-" + (length + 3) + "s";
        return "Stats for user **%s**%n".formatted(username) +
                "```prolog" +
                format.formatted(" ", "Solo", "Duo", "Squad") +
                format.formatted("Top 1",
                        stats.getSoloStats().getTop1(), stats.getDuoStats().getTop1(), stats.getSquadStats().getTop1()) +
                format.formatted("K/D season",
                        stats.getSeasonSoloStats().getRatio(), stats.getSeasonDuoStats().getRatio(),
                        stats.getSeasonSquadStats().getRatio()) +
                format.formatted("K/D lifetime",
                        stats.getSoloStats().getRatio(), stats.getDuoStats().getRatio(), stats.getSquadStats().getRatio()) +
                "```";
    }

}
