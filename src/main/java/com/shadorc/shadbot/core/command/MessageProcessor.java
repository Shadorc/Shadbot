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
import discord4j.rest.util.Snowflake;
import io.prometheus.client.Counter;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class MessageProcessor {

    private static final Counter COMMAND_USAGE_COUNTER = Counter.build()
            .namespace("shadbot")
            .name("command_usage")
            .help("Command usage")
            .labelNames("command")
            .register();

    public static Mono<Void> processEvent(MessageCreateEvent event) {
        return Mono.justOrEmpty(event.getGuildId())
                // This is a private channel, there is no guild ID
                .switchIfEmpty(MessageProcessor.processPrivateMessage(event).then(Mono.empty()))
                .flatMap(guildId -> DatabaseManager.getGuilds().getDBGuild(guildId))
                // The content is not a Webhook
                .zipWith(Mono.justOrEmpty(event.getMessage().getContent()))
                .flatMap(tuple -> MessageProcessor.processCommand(event, tuple.getT1(), tuple.getT2()));
    }

    private static Mono<Void> processCommand(MessageCreateEvent event, DBGuild dbGuild, String content) {
        return Mono.justOrEmpty(event.getMember())
                // The author is not a bot or is not the bot used for auto-testing
                .filter(member -> !member.isBot() || member.getId().equals(Config.TESTBOT_ID))
                .flatMap(member -> member.getRoles().collectList())
                .zipWith(event.getGuild().map(Guild::getOwnerId))
                // The role is allowed or the author is the guild's owner
                .filter(tuple -> dbGuild.getSettings().hasAllowedRole(tuple.getT1())
                        || event.getMessage().getAuthor().map(User::getId).map(tuple.getT2()::equals).orElse(false))
                // The channel is allowed
                .flatMap(ignored -> event.getMessage().getChannel())
                .filter(channel -> dbGuild.getSettings().isTextChannelAllowed(channel.getId()))
                // The message starts with the correct prefix
                .flatMap(ignored -> MessageProcessor.getPrefix(dbGuild, content))
                // Execute the command
                .flatMap(prefix -> MessageProcessor.executeCommand(dbGuild, new Context(event, prefix)));
    }

    private static Mono<Void> processPrivateMessage(MessageCreateEvent event) {
        final String text = String.format("Hello !"
                        + "%nCommands only work in a server but you can see help using `%shelp`."
                        + "%nIf you have a question, a suggestion or if you just want to talk, don't hesitate to "
                        + "join my support server : %s",
                Config.DEFAULT_PREFIX, Config.SUPPORT_SERVER_URL);

        final String content = event.getMessage().getContent();
        if (content.startsWith(String.format("%shelp", Config.DEFAULT_PREFIX))) {
            return CommandManager.getInstance().getCommand("help")
                    .execute(new Context(event, Config.DEFAULT_PREFIX));
        }

        return event.getMessage()
                .getChannel()
                .flatMapMany(channel -> channel.getMessagesBefore(Snowflake.of(Instant.now())))
                .take(50)
                .map(Message::getContent)
                .flatMap(Mono::justOrEmpty)
                .collectList()
                .filter(list -> list.stream().noneMatch(text::equalsIgnoreCase))
                .flatMap(ignored -> event.getMessage().getChannel())
                .flatMap(channel -> DiscordUtils.sendMessage(text, channel))
                .then();
    }

    private static Mono<String> getPrefix(DBGuild dbGuild, String content) {
        final String prefix = dbGuild.getSettings().getPrefix();
        if (content.startsWith(prefix)) {
            return Mono.just(prefix);
        }
        if (content.equalsIgnoreCase(String.format("%sprefix", Config.DEFAULT_PREFIX))) {
            return Mono.just(Config.DEFAULT_PREFIX);
        }
        return Mono.empty();
    }

    private static boolean isRateLimited(Context context, BaseCmd cmd) {
        return cmd.getRateLimiter()
                .map(rateLimiter -> rateLimiter.isLimitedAndWarn(context.getChannelId(), context.getMember()))
                .orElse(false);
    }

    private static Mono<Void> executeCommand(DBGuild dbGuild, Context context) {
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

        COMMAND_USAGE_COUNTER.labels(command.getName()).inc();

        return context.getPermissions()
                .collectList()
                // The author has the permission to execute this command
                .filter(userPerms -> userPerms.contains(command.getPermission()))
                .switchIfEmpty(context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(
                                String.format(Emoji.ACCESS_DENIED + " (**%s**) You do not have the permission to " +
                                        "execute this command.", context.getUsername()), channel)
                                .then(Mono.empty())))
                // The command is allowed in the guild and the user is not rate limited
                .filter(ignored -> dbGuild.getSettings().isCommandAllowed(command)
                        && !MessageProcessor.isRateLimited(context, command))
                .flatMap(ignored -> command.execute(context))
                .onErrorResume(err -> ExceptionHandler.handleCommandError(err, command, context));
    }

}
