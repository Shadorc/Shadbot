package com.shadorc.shadbot.command.info;

import com.shadorc.shadbot.core.command.*;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.Settings;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Consumer;

public class HelpCmd extends BaseCmd {

    public HelpCmd() {
        super(CommandCategory.INFO, "help", "Show the list of available commands");
        this.addOption("command", "Show help about a specific command", false, ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        final Optional<String> cmdName = context.getOptionAsString("command");
        if (cmdName.isPresent()) {
            final BaseCmd cmd = CommandManager.getInstance().getCommand(cmdName.orElseThrow());
            if (cmd == null) {
                return Mono.empty();
            }
            return context.createFollowupMessage(cmd.getHelp(context));
        }

        return context.getPermissions()
                .collectList()
                .flatMap(authorPermissions -> HelpCmd.getMultiMap(context, authorPermissions))
                .map(map -> HelpCmd.formatEmbed(map, context.getAuthorAvatarUrl()))
                .flatMap(context::createFollowupMessage);
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(Map<CommandCategory, Collection<String>> map, String avatarUrl) {
        return ShadbotUtil.getDefaultEmbed(
                embed -> {
                    embed.setAuthor("Shadbot Help", "https://github.com/Shadorc/Shadbot/wiki/Commands", avatarUrl);
                    embed.setDescription(String.format("Any issues, questions or suggestions ?"
                                    + " Join the [support server.](%s)"
                                    + "%nI need your help to [keep Shadbot alive!](%s)",
                            Config.SUPPORT_SERVER_URL, Config.PATREON_URL));
                    embed.setFooter("Click on the title to get a detailed list", "https://i.imgur.com/eaWQxvS.png");

                    for (final CommandCategory category : CommandCategory.values()) {
                        if (!map.getOrDefault(category, Collections.emptyList()).isEmpty()) {
                            embed.addField(String.format("%s Commands", category.getName()),
                                    String.join(" ", map.get(category)), false);
                        }
                    }
                });
    }

    private static Mono<Map<CommandCategory, Collection<String>>> getMultiMap(Context context, List<CommandPermission> authorPermissions) {
        final Mono<Boolean> getIsDm = context.getChannel()
                .map(Channel::getType)
                .map(Channel.Type.DM::equals)
                .cache();


        final Mono<Settings> getSettings = DatabaseManager.getGuilds()
                .getSettings(context.getGuildId())
                .cache();

        // Iterates over all commands...
        return Flux.fromIterable(CommandManager.getInstance().getCommands().values())
                // ... and removes duplicate ...
                .distinct()
                // ... and removes commands that the author cannot use ...
                .filter(cmd -> authorPermissions.contains(cmd.getPermission()))
                // ... and removes commands that are not allowed by the guild
                .filterWhen(cmd -> BooleanUtils.or(getIsDm,
                        getSettings.map(settings -> settings.isCommandAllowed(cmd) && settings.isCommandAllowedInChannel(cmd, context.getChannelId()))
                                .defaultIfEmpty(true)))
                .collectMultimap(BaseCmd::getCategory, cmd -> String.format("`/%s`", cmd.getName()));
    }

}
