package me.shadorc.shadbot.command.gamestats;

import discord4j.core.spec.EmbedCreateSpec;
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
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

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
                    args.size() == 1 ? OverwatchCmd.getResponse(args.get(0)) : OverwatchCmd.getResponse(args.get(0), args.get(1));

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

    private static Tuple3<Platform, ProfileResponse, StatsResponse> getResponse(String battletag) {
        for (final Platform platform : Platform.values()) {
            final Tuple3<Platform, ProfileResponse, StatsResponse> response = OverwatchCmd.getResponse(platform.toString(), battletag);
            if (response != null) {
                return response;
            }
        }

        throw new CommandException(String.format("Platform not found. Try again specifying it. %s",
                FormatUtils.options(Platform.class)));
    }

    private static Tuple3<Platform, ProfileResponse, StatsResponse> getResponse(String platformStr, String battletag) {
        final String username = battletag.replace("#", "-");
        final Platform platform = Utils.parseEnum(Platform.class, platformStr,
                new CommandException(String.format("`%s` is not a valid Platform. %s",
                        platformStr, FormatUtils.options(Platform.class))));

        final ProfileResponse profile = NetUtils.readValue(OverwatchCmd.getUrl("profile", platform, username), ProfileResponse.class);
        if (profile.getMessage().map("Error: Profile not found"::equals).orElse(false)) {
            return null;
        }
        final StatsResponse stats = NetUtils.readValue(OverwatchCmd.getUrl("stats", platform, username), StatsResponse.class);
        return Tuples.of(platform, profile, stats);
    }

    private static String getUrl(String endpoint, Platform platform, String username) {
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
