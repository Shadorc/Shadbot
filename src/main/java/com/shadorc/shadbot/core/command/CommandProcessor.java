package com.shadorc.shadbot.core.command;

import com.shadorc.shadbot.core.ratelimiter.RateLimiter;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.guild.entity.DBGuild;
import com.shadorc.shadbot.db.guild.GuildManager;
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
        final DBGuild dbGuild = GuildManager.getInstance().getDBGuild(guildId);
        return Mono.justOrEmpty(event.getMember())
                // The author is not a bot
                .filter(member -> !member.isBot())
                .flatMap(member -> member.getRoles().collectList())
                .zipWith(event.getGuild().map(Guild::getOwnerId))
                // The role is allowed or the author is the guild's owner
                .filter(tuple -> dbGuild.getSettings().hasAllowedRole(tuple.getT1())
                        || event.getMessage().getAuthor().map(User::getId).map(tuple.getT2()::equals).orElse(false))
                // The channel is allowed
                .flatMap(ignored -> event.getMessage().getChannel())
                .filter(channel -> dbGuild.getSettings().isTextChannelAllowed(channel.getId()))
                // The message starts with the correct prefix
                .flatMap(ignored -> this.checkPrefix(dbGuild, content))
                // Execute the command
                .flatMap(prefix -> this.executeCommand(new Context(event, prefix)));
    }

    private Mono<String> checkPrefix(DBGuild dbGuild, String content) {
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
        final Optional<RateLimiter> rateLimiter = cmd.getRateLimiter();
        if (rateLimiter.isEmpty()) {
            return false;
        }

        return rateLimiter.get().isLimitedAndWarn(context.getChannelId(), context.getMember());
    }

    private Mono<Void> executeCommand(Context context) {
        final BaseCmd command = CommandManager.getInstance().getCommand(context.getCommandName());
        if (command == null) {
            return Mono.empty();
        }

        if (!command.isEnabled()) {
            return context.getChannel()
                    .flatMap(channel -> DiscordUtils.sendMessage(
                            String.format(Emoji.ACCESS_DENIED + " (**%s**) Sorry, this command is temporary disabled. " +
                                            "Do not hesitate to join the support server (<%s>) if you have any questions.",
                                    context.getUsername(), Config.SUPPORT_SERVER_URL), channel))
                    .then();
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
                .filter(cmd -> GuildManager.getInstance().getDBGuild(context.getGuildId()).getSettings().isCommandAllowed(cmd))
                // The user is not rate limited
                .filter(cmd -> !this.isRateLimited(context, cmd))
                .flatMap(cmd -> cmd.execute(context))
                .onErrorResume(err -> ExceptionHandler.handleCommandError(err, command, context));
    }

    private Mono<Void> processPrivateMessage(MessageCreateEvent event) {
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
