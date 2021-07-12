package com.shadorc.shadbot.command.standalone;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.*;
import com.shadorc.shadbot.core.i18n.I18nContext;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.database.guilds.entity.Settings;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HelpCmd extends Cmd {

    public HelpCmd() {
        super(CommandCategory.INFO, "help", "Show the list of available commands");
        this.addOption(option -> option.name("command")
                .description("Show help about a specific command")
                .required(false)
                .type(ApplicationCommandOptionType.STRING.getValue()));
    }

    @Override
    public Mono<?> execute(Context context) {
        final Optional<String> cmdNameOpt = context.getOptionAsString("command");
        if (cmdNameOpt.isPresent()) {
            final String cmdName = cmdNameOpt.orElseThrow();
            final Cmd cmd = CommandManager.getCommand(cmdName);
            if (cmd == null) {
                return Mono.error(new CommandException(context.localize("help.cmd.not.found")
                        .formatted(cmdName)));
            }
            return context.createFollowupMessage(cmd.getHelp(context));
        }

        return context.getPermissions()
                .collectList()
                .flatMap(authorPermissions -> HelpCmd.getMultiMap(context, authorPermissions))
                .map(map -> HelpCmd.formatEmbed(context, map, context.getAuthorAvatar()))
                .flatMap(context::createFollowupMessage);
    }

    private static EmbedCreateSpec formatEmbed(I18nContext context, Map<CommandCategory, Collection<String>> map,
                                               String avatarUrl) {
        final EmbedCreateSpec.Builder embed = ShadbotUtil.createEmbedBuilder()
                .author(context.localize("help.title"), "https://github.com/Shadorc/Shadbot/wiki/Commands", avatarUrl)
                .description(context.localize("help.description")
                        .formatted(Config.SUPPORT_SERVER_URL, Config.PATREON_URL))
                .footer(context.localize("help.footer"), "https://i.imgur.com/eaWQxvS.png");

        for (final CommandCategory category : CommandCategory.values()) {
            final Collection<String> cmds = map.get(category);
            if (cmds != null && !cmds.isEmpty()) {
                embed.addField(context.localize("help.field.title").formatted(category.getName()),
                        String.join(" ", cmds), false);
            }
        }

        return embed.build();
    }

    private static Mono<Map<CommandCategory, Collection<String>>> getMultiMap(Context context, List<CommandPermission> authorPermissions) {
        final Settings settings = context.getDbGuild().getSettings();
        return Flux.fromIterable(CommandManager.getCommands())
                // Removes commands that the author cannot use
                .filter(cmd -> authorPermissions.contains(cmd.getPermission()))
                // Removes commands that are not allowed by the guild
                .filter(cmd -> settings.isCommandAllowed(cmd)
                        && settings.isCommandAllowedInChannel(cmd, context.getChannelId()))
                .map(cmd -> Tuples.of(cmd.getCategory(), cmd instanceof SubCmd subCmd ? subCmd.getFullName() : cmd.getName()))
                .collectMultimap(Tuple2::getT1, tuples -> "`/%s`".formatted(tuples.getT2()));
    }

    // Essential part of Shadbot (Thanks to @Bluerin)
    private String howToDoAChocolateCake() {
        final String meal = "50g farine";
        final String chocolate = "200g chocolat";
        final String eggs = "3 oeufs";
        final String sugar = "100g sucre";
        final String butter = "100g beurre";
        return "Mélanger " + meal + " " + sugar + " " + eggs + " dans un saladier." +
                "\nFaire fondre au bain-marie " + chocolate + " " + butter +
                "\nRajouter le chocolat et le beurre dans le saladier." +
                "\nVerser le mélange dans un moule et enfourner pendant 25min à 180°C.";
    }

}
