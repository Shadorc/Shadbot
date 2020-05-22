package com.shadorc.shadbot.command.hidden;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.premium.RelicType;
import com.shadorc.shadbot.db.premium.entity.Relic;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.TimeUtils;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ImmutableEmbedFieldData;
import discord4j.discordjson.possible.Possible;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class RelicStatusCmd extends BaseCmd {

    public RelicStatusCmd() {
        super(CommandCategory.HIDDEN, List.of("contributor_status", "donator_status", "relic_status"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        return DatabaseManager.getPremium()
                .getUserRelics(context.getAuthorId())
                .flatMap(relic -> RelicStatusCmd.getRelicAndGuild(context, relic))
                .map(TupleUtils.function((relic, optGuild) -> {
                    final StringBuilder descBuilder = new StringBuilder(String.format("**ID:** %s", relic.getId()));

                    optGuild.ifPresent(guild ->
                            descBuilder.append(String.format("%n**Guild:** %s (ID: %d)",
                                    guild.getName(), guild.getId().asLong())));

                    descBuilder.append(String.format("%n**Duration:** %s",
                            FormatUtils.customDate(relic.getDuration())));

                    if (!relic.isExpired()) {
                        relic.getActivation()
                                .ifPresent(activation -> {
                                    final Duration durationLeft = relic.getDuration()
                                            .minusMillis(TimeUtils.getMillisUntil(activation.toEpochMilli()));
                                    descBuilder.append(String.format("%n**Expires in:** %s",
                                            FormatUtils.customDate(durationLeft)));
                                });
                    }

                    final StringBuilder titleBuilder = new StringBuilder();
                    if (relic.getType() == RelicType.GUILD) {
                        titleBuilder.append("Legendary ");
                    }
                    titleBuilder.append(String.format("Relic (%s)", relic.isExpired() ? "Expired" : "Activated"));

                    return ImmutableEmbedFieldData.of(titleBuilder.toString(), descBuilder.toString(), Possible.of(false));
                }))
                .collectList()
                .filter(list -> !list.isEmpty())
                .map(fields -> DiscordUtils.getDefaultEmbed()
                        .andThen(embed -> {
                            embed.setAuthor("Contributor Status", null, context.getAvatarUrl())
                                    .setThumbnail("https://i.imgur.com/R0N6kW3.png");

                            fields.forEach(field -> embed.addField(field.name(), field.value(), field.inline().get()));
                        }))
                .flatMap(embed -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(embed, channel)))
                .switchIfEmpty(context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(
                                String.format(Emoji.INFO + " (**%s**) You are not a donator. If you like Shadbot, "
                                                + "you can help me keep it alive by making a donation on <%s>."
                                                + "%nAll donations are important and really help me %s",
                                        context.getUsername(), Config.PATREON_URL, Emoji.HEARTS), channel)))
                .then();
    }

    private static Mono<Tuple2<Relic, Optional<Guild>>> getRelicAndGuild(Context context, Relic relic) {
        final Mono<Optional<Guild>> getGuild = Mono.justOrEmpty(relic.getGuildId())
                .flatMap(context.getClient()::getGuildById)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty());

        return Mono.zip(Mono.just(relic), getGuild);
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Show your contributor status.")
                .build();
    }
}
