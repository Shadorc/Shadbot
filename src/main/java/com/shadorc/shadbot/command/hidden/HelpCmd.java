package com.shadorc.shadbot.command.hidden;

import com.shadorc.shadbot.core.command.*;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.db.guilds.entity.Settings;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class HelpCmd extends BaseCmd {

    public HelpCmd() {
        super(CommandCategory.HIDDEN, List.of("help"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        if (context.getArg().isPresent()) {
            final BaseCmd cmd = CommandManager.getInstance().getCommand(context.getArg().get());
            if (cmd == null) {
                return Mono.empty();
            }

            return context.getChannel()
                    .flatMap(channel -> DiscordUtils.sendMessage(cmd.getHelp(context), channel))
                    .then();
        }

        return context.getPermissions()
                .collectList()
                .flatMap(authorPermissions -> getMultiMap(context, authorPermissions))
                .map(map -> DiscordUtils.getDefaultEmbed()
                        .andThen(embed -> {
                            embed.setAuthor("Shadbot Help", null, context.getAvatarUrl())
                                    .setDescription(String.format("Any issues, questions or suggestions ?"
                                                    + " Join the [support server.](%s)"
                                                    + "%nGet more information by using `%s%s <command>`.",
                                            Config.SUPPORT_SERVER_URL, context.getPrefix(), this.getName()));

                            for (final CommandCategory category : CommandCategory.values()) {
                                if (!map.getOrDefault(category, Collections.emptyList()).isEmpty()
                                        && category != CommandCategory.HIDDEN) {
                                    embed.addField(String.format("%s Commands", category),
                                            String.join(" ", map.get(category)), false);
                                }
                            }
                        }))
                .flatMap(embedConsumer -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(embedConsumer, channel)))
                .then();
    }

    private static Mono<Map<CommandCategory, Collection<String>>> getMultiMap(Context context,
                                                                              List<CommandPermission> authorPermissions) {
        final Mono<Boolean> getIsDm = context.getChannel()
                .map(Channel::getType)
                .map(Channel.Type.DM::equals)
                .cache();

        // Get the settings of the guild or empty if this is a DM
        final Mono<Settings> getSettings = Mono.justOrEmpty(context.getEvent().getGuildId())
                .flatMap(DatabaseManager.getGuilds()::getDBGuild)
                .map(DBGuild::getSettings)
                .cache();

        // Iterates over all commands...
        return Flux.fromIterable(CommandManager.getInstance().getCommands().values())
                // ... and removes duplicate ...
                .distinct()
                // ... and removes commands that the author cannot use ...
                .filter(cmd -> authorPermissions.contains(cmd.getPermission()))
                // ... and removes commands that are not allowed by the guild
                .filterWhen(cmd -> Mono.zip(getIsDm,
                        getSettings.map(settings -> settings.isCommandAllowed(cmd)).defaultIfEmpty(true))
                        .map(tuple -> tuple.getT1() || tuple.getT2()))
                .collectMultimap(BaseCmd::getCategory, cmd -> String.format("`%s%s`", context.getPrefix(), cmd.getName()));
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Show the list of available commands.")
                .build();
    }

}
