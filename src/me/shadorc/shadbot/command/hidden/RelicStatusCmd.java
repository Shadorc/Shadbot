package me.shadorc.shadbot.command.hidden;

import discord4j.common.json.EmbedFieldEntity;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.premium.Relic;
import me.shadorc.shadbot.data.premium.Relic.RelicType;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

public class RelicStatusCmd extends BaseCmd {

    public RelicStatusCmd() {
        super(CommandCategory.HIDDEN, List.of("contributor_status", "donator_status", "relic_status"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<Relic> relics = Shadbot.getPremium().getRelicsForUser(context.getAuthorId());
        if (relics.isEmpty()) {
            return context.getChannel()
                    .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.INFO + " (**%s**) You are not a donator. If you like Shadbot, "
                                    + "you can help me keep it alive by making a donation on **%s**."
                                    + "%nAll donations are important and really help me %s",
                            context.getUsername(), Config.PATREON_URL, Emoji.HEARTS), channel))
                    .then();
        }

        return Flux.fromIterable(relics)
                .map(relic -> {
                    final StringBuilder contentBld = new StringBuilder(String.format("**ID:** %s", relic.getId()));

                    relic.getGuildId().ifPresent(guildId -> contentBld.append(String.format("%n**Guild ID:** %d", guildId.asLong())));

                    contentBld.append(String.format("%n**Duration:** %d days", relic.getDuration().toDays()));
                    if (!relic.isExpired() && relic.getActivationInstant().isPresent()) {
                        final Duration durationLeft = relic.getDuration().minusMillis(TimeUtils.getMillisUntil(relic.getActivationInstant().get().toEpochMilli()));
                        contentBld.append(String.format("%n**Expires in:** %d days", durationLeft.toDays()));
                    }

                    final StringBuilder titleBld = new StringBuilder();
                    if (relic.getType().equals(RelicType.GUILD.toString())) {
                        titleBld.append("Legendary ");
                    }
                    titleBld.append(String.format("Relic (%s)", relic.isExpired() ? "Expired" : "Activated"));

                    return new EmbedFieldEntity(titleBld.toString(), contentBld.toString(), false);
                })
                .collectList()
                .map(fields -> EmbedUtils.getDefaultEmbed()
                        .andThen(embed -> {
                            embed.setAuthor("Contributor Status", null, context.getAvatarUrl())
                                    .setThumbnail("https://orig00.deviantart.net/24e1/f/2015/241/8/7/relic_fragment_by_yukimemories-d97l8c8.png");

                            fields
                                    .forEach(field -> embed.addField(field.getName(), field.getValue(), field.isInline()));
                        }))
                .flatMap(embed -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(embed, channel)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show your contributor status.")
                .build();
    }
}
