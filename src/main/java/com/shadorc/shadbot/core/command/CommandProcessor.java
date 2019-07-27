package com.shadorc.shadbot.core.command;

import com.shadorc.shadbot.Config;
import com.shadorc.shadbot.core.ratelimiter.RateLimiter;
import com.shadorc.shadbot.data.database.DBGuild;
import com.shadorc.shadbot.data.database.DatabaseManager;
import com.shadorc.shadbot.data.stats.StatsManager;
import com.shadorc.shadbot.data.stats.enums.CommandEnum;
import com.shadorc.shadbot.data.stats.enums.VariousEnum;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.ExceptionHandler;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;

public class CommandProcessor {

    private static CommandProcessor instance;

    static {
        CommandProcessor.instance = new CommandProcessor();
    }

    private CommandProcessor() {
    }

    public Mono<Void> processMessageEvent(MessageCreateEvent event) {
        // This is not a private channel
        if (event.getGuildId().isEmpty()) {
            return this.processPrivateMessage(event);
        }

        // The content is not a Webhook
        if (event.getMessage().getContent().isEmpty()) {
            return Mono.empty();
        }

        final String content = event.getMessage().getContent().get();
        final Snowflake guildId = event.getGuildId().get();
        final DBGuild dbGuild = DatabaseManager.getInstance().getDBGuild(guildId);
        return Mono.justOrEmpty(event.getMember())
                // The author is not a bot
                .filter(member -> !member.isBot())
                // The role is allowed
                .flatMap(member -> member.getRoles().collectList())
                .filter(dbGuild::hasAllowedRole)
                // The channel is allowed
                .flatMap(ignored -> event.getMessage().getChannel())
                .filter(channel -> dbGuild.isTextChannelAllowed(channel.getId()))
                // The message starts with the correct prefix
                .map(ignored -> dbGuild.getPrefix())
                .filter(content::startsWith)
                // Execute the command
                .flatMap(prefix -> this.executeCommand(new Context(event, prefix)));
    }

    private boolean isRateLimited(Context context, BaseCmd cmd) {
        final Optional<RateLimiter> rateLimiter = cmd.getRateLimiter();
        if (rateLimiter.isEmpty()) {
            return false;
        }

        if (rateLimiter.get().isLimitedAndWarn(context.getChannelId(), context.getMember())) {
            StatsManager.COMMAND_STATS.log(CommandEnum.COMMAND_LIMITED, cmd);
            return true;
        }
        return false;
    }

    private Mono<Void> executeCommand(Context context) {
        final BaseCmd command = CommandManager.getInstance().getCommand(context.getCommandName());
        if (command == null) {
            return Mono.empty();
        }

        return context.getPermissions()
                .collectList()
                // The author has the permission to execute this command
                .filter(userPerms -> userPerms.contains(command.getPermission()))
                .switchIfEmpty(context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(
                                String.format(Emoji.ACCESS_DENIED + " (**%s**) You do not have the permission to execute this command.",
                                        context.getUsername()), channel)
                                .then(Mono.empty())))
                .flatMap(perm -> Mono.just(command))
                // The command is allowed in the guild
                .filter(cmd -> DatabaseManager.getInstance().getDBGuild(context.getGuildId()).isCommandAllowed(cmd))
                // The user is not rate limited
                .filter(cmd -> !this.isRateLimited(context, cmd))
                .flatMap(cmd -> cmd.execute(context))
                .onErrorResume(err -> ExceptionHandler.handleCommandError(err, command, context))
                .doOnTerminate(() -> {
                    StatsManager.COMMAND_STATS.log(CommandEnum.COMMAND_USED, command);
                    StatsManager.VARIOUS_STATS.log(VariousEnum.COMMANDS_EXECUTED);
                });
    }

    private Mono<Void> processPrivateMessage(MessageCreateEvent event) {
        StatsManager.VARIOUS_STATS.log(VariousEnum.PRIVATE_MESSAGES_RECEIVED);

        final String text = String.format("Hello !"
                        + "%nCommands only work in a server but you can see help using `%shelp`."
                        + "%nIf you have a question, a suggestion or if you just want to talk, don't hesitate to "
                        + "join my support server : %s",
                Config.DEFAULT_PREFIX, Config.SUPPORT_SERVER_URL);

        final String content = event.getMessage().getContent().orElse("");
        if (content.startsWith(Config.DEFAULT_PREFIX + "help")) {
            return CommandManager.getInstance().getCommand("help")
                    .execute(new Context(event, Config.DEFAULT_PREFIX));
        } else {
            return event.getMessage()
                    .getChannel()
                    .flatMapMany(channel -> channel.getMessagesBefore(Snowflake.of(Instant.now())))
                    .map(Message::getContent)
                    .flatMap(Mono::justOrEmpty)
                    .take(50)
                    .collectList()
                    .filter(list -> list.stream().noneMatch(text::equalsIgnoreCase))
                    .flatMap(ignored -> event.getMessage().getChannel())
                    .flatMap(channel -> DiscordUtils.sendMessage(text, channel))
                    .then();
        }
    }

    public static CommandProcessor getInstance() {
        return CommandProcessor.instance;
    }

}
