package me.shadorc.shadbot.command.gamestats;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.api.gamestats.overwatch.profile.ProfileResponse;
import me.shadorc.shadbot.api.gamestats.overwatch.stats.Quickplay;
import me.shadorc.shadbot.api.gamestats.overwatch.stats.StatsResponse;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.object.message.LoadingMessage;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class OverwatchCmd extends BaseCmd {

    private enum Platform {
        PC, PSN, XBL;
    }

    public OverwatchCmd() {
        super(CommandCategory.GAMESTATS, List.of("overwatch"), "ow");
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(1, 2);

        final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());
        return Mono.fromCallable(() -> {
            final Tuple3<Platform, ProfileResponse, StatsResponse> response =
                    args.size() == 1 ? this.getResponse(args.get(0)) : this.getResponse(args.get(0), args.get(1));

            if (response == null) {
                return loadingMsg.setContent(
                        String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) Overwatch player not found.",
                                context.getUsername()));
            }

            final Platform platform = response.getT1();
            final ProfileResponse profile = response.getT2();
            final Quickplay topHeroes = response.getT3().getStats().getTopHeroes().getQuickplay();

            if (profile.isPrivate()) {
                return loadingMsg.setContent(
                        String.format(Emoji.ACCESS_DENIED + " (**%s**) This profile is private.",
                                context.getUsername()));
            }

            return loadingMsg.setEmbed(EmbedUtils.getDefaultEmbed()
                    .andThen(embed -> embed.setAuthor("Overwatch Stats (Quickplay)", String.format("https://playoverwatch.com/en-gb/career/%s/%s",
                            StringUtils.toLowerCase(platform), profile.getUsername()), context.getAvatarUrl())
                            .setThumbnail(profile.getPortrait())
                            .setDescription(String.format("Stats for user **%s**", profile.getUsername()))
                            .addField("Level", profile.getLevel(), true)
                            .addField("Competitive rank", profile.getRank(), true)
                            .addField("Games won", profile.getGames().getQuickplayWon(), true)
                            .addField("Time played", profile.getQuickplayPlaytime(), true)
                            .addField("Top hero (Time played)", topHeroes.getPlayed(), true)
                            .addField("Top hero (Eliminations per life)", topHeroes.getEliminationsPerLife(), true)));
        })
                .flatMap(LoadingMessage::send)
                .doOnTerminate(loadingMsg::stopTyping)
                .then();
    }

    private Tuple3<Platform, ProfileResponse, StatsResponse> getResponse(String battletag) throws IOException {
        for (final Platform platform : Platform.values()) {
            final Tuple3<Platform, ProfileResponse, StatsResponse> response = this.getResponse(platform.toString(), battletag);
            if (response != null) {
                return response;
            }
        }

        throw new CommandException(String.format("Platform not found. Try again specifying it. %s",
                FormatUtils.options(Platform.class)));
    }

    private Tuple3<Platform, ProfileResponse, StatsResponse> getResponse(String platformStr, String battletag) throws IOException {
        final String username = battletag.replace("#", "-");
        final Platform platform = Utils.parseEnum(Platform.class, platformStr,
                new CommandException(String.format("`%s` is not a valid Platform. %s",
                        platformStr, FormatUtils.options(Platform.class))));

        final ProfileResponse profile = Utils.MAPPER.readValue(NetUtils.getJSON(this.getUrl("profile", platform, username)), ProfileResponse.class);
        if (profile.getGames().getQuickplay().isEmpty()) {
            LogUtils.warn(Shadbot.getClient(), "Overwatch debug: " + profile.toString());
        }
        if (profile.getMessage().map("Error: Profile not found"::equals).orElse(false)) {
            return null;
        }
        final StatsResponse stats = Utils.MAPPER.readValue(NetUtils.getJSON(this.getUrl("stats", platform, username)), StatsResponse.class);
        return Tuples.of(platform, profile, stats);
    }

    private String getUrl(String endpoint, Platform platform, String username) {
        return String.format("http://overwatchy.com/%s/%s/global/%s",
                endpoint, StringUtils.toLowerCase(platform), username);
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show player's stats for Overwatch.")
                .addArg("platform", String.format("user's platform (%s)", FormatUtils.format(Platform.class, ", ")), true)
                .addArg("username", "case sensitive", false)
                .addField("Info", "**platform** is automatically detected if nothing is specified.", false)
                .setExample(String.format("%s%s pc Shadorc#2503", context.getPrefix(), context.getCommandName()))
                .build();
    }

}
