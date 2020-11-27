package com.shadorc.shadbot.command.gamestats;

import com.shadorc.shadbot.api.json.gamestats.fortnite.FortniteResponse;
import com.shadorc.shadbot.api.json.gamestats.fortnite.Stats;
import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.EnumUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.NetUtils;
import com.shadorc.shadbot.utils.ShadbotUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class FortniteCmd extends BaseCmd {

    private enum Platform {
        PC, XBL, PSN;
    }

    public FortniteCmd() {
        super(CommandCategory.GAMESTATS, List.of("fortnite"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(2);

        final Platform platform = EnumUtils.parseEnum(Platform.class, args.get(0),
                new CommandException(String.format("`%s` is not a valid Platform. %s",
                        args.get(0), FormatUtils.options(Platform.class))));

        final String epicNickname = args.get(1);
        final String encodedNickname = NetUtils.encode(epicNickname.replace(" ", "%20"));
        final String url = String.format("https://api.fortnitetracker.com/v1/profile/%s/%s",
                platform.toString().toLowerCase(), encodedNickname);

        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());

        return updatableMsg.setContent(String.format(Emoji.HOURGLASS + " (**%s**) Loading Fortnite stats...", context.getUsername()))
                .send()
                .then(RequestHelper.create(url)
                        .addHeaders("TRN-Api-Key", CredentialManager.getInstance().get(Credential.FORTNITE_API_KEY))
                        .toMono(FortniteResponse.class))
                .map(fortnite -> {
                    if (fortnite.getError().map("Player Not Found"::equals).orElse(false)) {
                        throw Exceptions.propagate(new IOException("HTTP Error 400. The request URL is invalid."));
                    }

                    final int length = 8;
                    final String format = "%n%-" + (length + 5) + "s %-" + length + "s %-" + length + "s %-" + (length + 3) + "s";
                    final Stats stats = fortnite.getStats();

                    final String description = String.format("Stats for user **%s**%n", epicNickname)
                            + "```prolog"
                            + String.format(format, " ", "Solo", "Duo", "Squad")
                            + String.format(format, "Top 1", stats.getSoloStats().getTop1(),
                            stats.getDuoStats().getTop1(), stats.getSquadStats().getTop1())
                            + String.format(format, "K/D season", stats.getSeasonSoloStats().getRatio(),
                            stats.getSeasonDuoStats().getRatio(), stats.getSeasonSquadStats().getRatio())
                            + String.format(format, "K/D lifetime", stats.getSoloStats().getRatio(),
                            stats.getDuoStats().getRatio(), stats.getSquadStats().getRatio())
                            + "```";

                    return updatableMsg.setEmbed(ShadbotUtils.getDefaultEmbed()
                            .andThen(embed -> embed.setAuthor("Fortnite Stats",
                                    String.format("https://fortnitetracker.com/profile/%s/%s",
                                            platform.toString().toLowerCase(), encodedNickname),
                                    context.getAvatarUrl())
                                    .setThumbnail("https://i.imgur.com/8NrvS8e.png")
                                    .setDescription(description)));
                })
                .onErrorResume(err -> err.getMessage().contains("HTTP Error 400. The request URL is invalid.")
                                || err.getMessage().contains("wrong header"),
                        err -> Mono.just(updatableMsg.setContent(
                                String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) This user doesn't play Fortnite " +
                                                "on this platform or doesn't exist. Please make sure your spelling is" +
                                                " correct, or follow this guide if you play on Console: " +
                                                "<https://fortnitetracker.com/profile/search>",
                                        context.getUsername()))))
                .flatMap(UpdatableMessage::send)
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Show player's stats for Fortnite.")
                .addArg("platform", String.format("user's platform (%s)", FormatUtils.format(Platform.class, ", ")),
                        false)
                .addArg("epic-nickname", false)
                .setExample(String.format("`%s%s pc Shadbot`", context.getPrefix(), this.getName()))
                .build();
    }

}
