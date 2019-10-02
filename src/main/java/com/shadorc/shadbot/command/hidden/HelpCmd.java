package com.shadorc.shadbot.command.hidden;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandManager;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.database.DatabaseManager;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Channel.Type;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
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
                .flatMap(authorPerms -> Flux.fromIterable(CommandManager.getInstance().getCommands().values())
                        .distinct()
                        .filter(cmd -> authorPerms.contains(cmd.getPermission()))
                        .filterWhen(cmd -> context.getChannel().map(Channel::getType)
                                .map(type -> type == Type.DM || DatabaseManager.getInstance().getDBGuild(context.getGuildId()).isCommandAllowed(cmd)))
                        .collectMultimap(BaseCmd::getCategory, cmd -> String.format("`%s%s`", context.getPrefix(), cmd.getName())))
                .map(map -> DiscordUtils.getDefaultEmbed()
                        .andThen(embed -> {
                            embed.setAuthor("Shadbot Help", null, context.getAvatarUrl())
                                    .setDescription(String.format("Any issues, questions or suggestions ?"
                                                    + " Join the [support server.](%s)"
                                                    + "%nGet more information by using `%s%s <command>`.",
                                            Config.SUPPORT_SERVER_URL, context.getPrefix(), this.getName()));

                            for (final CommandCategory category : CommandCategory.values()) {
                                if (map.get(category) != null && !map.get(category).isEmpty() && category != CommandCategory.HIDDEN) {
                                    embed.addField(String.format("%s Commands", category.toString()), String.join(" ", map.get(category)), false);
                                }
                            }
                        }))
                .flatMap(embedConsumer -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(embedConsumer, channel)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show the list of available commands.")
                .build();
    }

}
