package com.shadorc.shadbot.command.hidden;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.premium.PremiumManager;
import com.shadorc.shadbot.db.premium.RelicType;
import com.shadorc.shadbot.db.premium.entity.Relic;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.TimeUtils;
import discord4j.common.json.EmbedFieldEntity;
import discord4j.core.spec.EmbedCreateSpec;
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
        final List<Relic> relics = PremiumManager.getInstance().getRelicsByUser(context.getAuthorId());
        if (relics.isEmpty()) {
            return context.getChannel()
                    .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.INFO + " (**%s**) You are not a donator. If you like Shadbot, "
                                    + "you can help me keep it alive by making a donation on <%s>."
                                    + "%nAll donations are important and really help me %s",
                            context.getUsername(), Config.PATREON_URL, Emoji.HEARTS), channel))
                    .then();
        }

        return Flux.fromIterable(relics)
                .map(relic -> {
                    final StringBuilder contentBld = new StringBuilder(String.format("**ID:** %s", relic.getId()));

                    relic.getGuildId().ifPresent(guildId -> contentBld.append(String.format("%n**Guild ID:** %d", guildId.asLong())));

                    contentBld.append(String.format("%n**Duration:** %s", FormatUtils.customDate(relic.getDuration())));
                    if (!relic.isExpired()) {
                        relic.getActivation()
                                .ifPresent(activation -> {
                                    final Duration durationLeft = relic.getDuration().minusMillis(TimeUtils.getMillisUntil(activation.toEpochMilli()));
                                    contentBld.append(String.format("%n**Expires in:** %s", FormatUtils.customDate(durationLeft)));
                                });
                    }

                    final StringBuilder titleBld = new StringBuilder();
                    if (relic.getType() == RelicType.GUILD) {
                        titleBld.append("Legendary ");
                    }
                    titleBld.append(String.format("Relic (%s)", relic.isExpired() ? "Expired" : "Activated"));

                    return new EmbedFieldEntity(titleBld.toString(), contentBld.toString(), false);
                })
                .collectList()
                .map(fields -> DiscordUtils.getDefaultEmbed()
                        .andThen(embed -> {
                            embed.setAuthor("Contributor Status", null, context.getAvatarUrl())
                                    .setThumbnail("https://i.imgur.com/R0N6kW3.png");

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
