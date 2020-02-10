package com.shadorc.shadbot.core.command;

import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.ExceptionHandler;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class MessageProcessor {

    private final MessageCreateEvent event;

    public MessageProcessor(MessageCreateEvent event) {
        this.event = event;
    }

    public Mono<Void> processMessage() {
        return Mono.justOrEmpty(this.event.getGuildId())
                .flatMap(guildId -> DatabaseManager.getGuilds().getDBGuild(guildId))
                // This is a private channel, there is no guild ID
                .switchIfEmpty(this.processPrivateMessage().then(Mono.empty()))
                // The content is not a Webhook
                .zipWith(Mono.justOrEmpty(this.event.getMessage().getContent()))
                .flatMap(tuple -> this.processCommand(tuple.getT1(), tuple.getT2()));
    }

    private Mono<Void> processCommand(DBGuild dbGuild, String content) {
        return Mono.justOrEmpty(this.event.getMember())
                // The author is not a bot
                .filter(member -> !member.isBot())
                .flatMap(member -> member.getRoles().collectList())
                .zipWith(this.event.getGuild().map(Guild::getOwnerId))
                // The role is allowed or the author is the guild's owner
                .filter(tuple -> dbGuild.getSettings().hasAllowedRole(tuple.getT1())
                        || this.event.getMessage().getAuthor().map(User::getId).map(tuple.getT2()::equals).orElse(false))
                // The channel is allowed
                .flatMap(ignored -> this.event.getMessage().getChannel())
                .filter(channel -> dbGuild.getSettings().isTextChannelAllowed(channel.getId()))
                // The message starts with the correct prefix
                .flatMap(ignored -> this.getPrefix(dbGuild, content))
                // Execute the command
                .flatMap(prefix -> this.executeCommand(dbGuild, new Context(this.event, prefix)));
    }

    private Mono<Void> processPrivateMessage() {
        final String text = String.format("Hello !"
                        + "%nCommands only work in a server but you can see help using `%shelp`."
                        + "%nIf you have a question, a suggestion or if you just want to talk, don't hesitate to "
                        + "join my support server : %s",
                Config.DEFAULT_PREFIX, Config.SUPPORT_SERVER_URL);

        final String content = this.event.getMessage().getContent().orElse("");
        if (content.startsWith(String.format("%shelp", Config.DEFAULT_PREFIX))) {
            return CommandManager.getInstance().getCommand("help")
                    .execute(new Context(this.event, Config.DEFAULT_PREFIX));
        }

        return this.event.getMessage()
                .getChannel()
                .flatMapMany(channel -> channel.getMessagesBefore(Snowflake.of(Instant.now())))
                .take(50)
                .map(Message::getContent)
                .flatMap(Mono::justOrEmpty)
                .collectList()
                .filter(list -> list.stream().noneMatch(text::equalsIgnoreCase))
                .flatMap(ignored -> this.event.getMessage().getChannel())
                .flatMap(channel -> DiscordUtils.sendMessage(text, channel))
                .then();
    }

    private Mono<String> getPrefix(DBGuild dbGuild, String content) {
        final String prefix = dbGuild.getSettings().getPrefix();
        if (content.startsWith(prefix)) {
            return Mono.just(prefix);
        }
        if (content.equalsIgnoreCase(String.format("%sprefix", Config.DEFAULT_PREFIX))) {
            return Mono.just(Config.DEFAULT_PREFIX);
        }
        return Mono.empty();
    }

    private boolean isRateLimited(Context context, BaseCmd cmd) {
        return cmd.getRateLimiter()
                .map(rateLimiter -> rateLimiter.isLimitedAndWarn(context.getChannelId(), context.getMember()))
                .orElse(false);
    }

    private Mono<Void> executeCommand(DBGuild dbGuild, Context context) {
        final BaseCmd command = CommandManager.getInstance().getCommand(context.getCommandName());
        // The command does not exist
        if (command == null) {
            return Mono.empty();
        }

        if (!command.isEnabled()) {
            return context.getChannel()
                    .flatMap(channel -> DiscordUtils.sendMessage(
                            String.format(Emoji.ACCESS_DENIED + " (**%s**) Sorry, this command is temporary disabled." +
                                            " Do not hesitate to join the support server (<%s>) if you have any " +
                                            "questions.",
                                    context.getUsername(), Config.SUPPORT_SERVER_URL), channel))
                    .then();
        }

        return DatabaseManager.getStats()
                .logCommand(command)
                .thenMany(context.getPermissions())
                .collectList()
                // The author has the permission to execute this command
                .filter(userPerms -> userPerms.contains(command.getPermission()))
                .switchIfEmpty(context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(
                                String.format(Emoji.ACCESS_DENIED + " (**%s**) You do not have the permission to " +
                                        "execute this command.", context.getUsername()), channel)
                                .then(Mono.empty())))
                .flatMap(ignored -> Mono.just(command))
                // The command is allowed in the guild
                .filter(cmd -> dbGuild.getSettings().isCommandAllowed(cmd))
                // The user is not rate limited
                .filter(cmd -> !this.isRateLimited(context, cmd))
                .flatMap(cmd -> cmd.execute(context))
                .onErrorResume(err -> ExceptionHandler.handleCommandError(err, command, context));
    }

}
