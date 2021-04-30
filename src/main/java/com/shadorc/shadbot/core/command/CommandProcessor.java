package com.shadorc.shadbot.core.command;

import com.shadorc.shadbot.core.ratelimiter.RateLimitResponse;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.ExceptionHandler;
import com.shadorc.shadbot.utils.ReactorUtil;
import discord4j.core.object.entity.Guild;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Mono;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class CommandProcessor {

    public static Mono<?> processCommand(Context context) {
        return Mono.just(context.getAuthor())
                // The role is allowed or the author is the guild's owner
                .filterWhen(ReactorUtil.filterWhenSwitchIfFalse(
                        member -> BooleanUtils.or(
                                member.getRoles().collectList().map(context.getDbGuild().getSettings()::hasAllowedRole),
                                member.getGuild().map(Guild::getOwnerId).map(member.getId()::equals)),
                        context.reply(Emoji.ACCESS_DENIED, context.localize("role.not.allowed"))))
                // The channel is allowed
                .filter(__ -> context.getDbGuild().getSettings().isTextChannelAllowed(context.getChannelId()))
                // Execute the command
                .flatMap(__ -> CommandProcessor.executeCommand(context));
    }

    private static Mono<?> executeCommand(Context context) {
        final BaseCmd command = CommandManager.getCommand(context.getLastCommandName());
        // The command does not exist
        if (command == null) {
            DEFAULT_LOGGER.error("{Guild ID: {}} Command {} not found",
                    context.getGuildId().asString(), context.getFullCommandName());
            return Mono.error(new RuntimeException("Command not found"));
        }

        // The command has been disabled
        if (!command.isEnabled()) {
            return context.reply(Emoji.ACCESS_DENIED, context.localize("command.disabled")
                    .formatted(Config.SUPPORT_SERVER_URL));
        }

        // This category is not allowed in this channel
        if (!context.getDbGuild().getSettings().isCommandAllowedInChannel(command, context.getChannelId())) {
            return context.reply(Emoji.ACCESS_DENIED, context.localize("command.channel.not.allowed"));
        }

        // This command is not allowed to this role
        if (!context.getDbGuild().getSettings().isCommandAllowedToRole(command, context.getAuthor().getRoleIds())) {
            return context.reply(Emoji.ACCESS_DENIED, context.localize("command.role.not.allowed"));
        }

        return context.getPermissions()
                .collectList()
                // The author has the permission to execute this command
                .filterWhen(ReactorUtil.filterSwitchIfFalse(
                        userPerms -> userPerms.contains(command.getPermission()),
                        context.reply(Emoji.ACCESS_DENIED, context.localize("command.missing.permission"))))
                // The command is allowed in the guild
                .filterWhen(ReactorUtil.filterSwitchIfFalse(
                        __ -> context.getDbGuild().getSettings().isCommandAllowed(command),
                        context.reply(Emoji.ACCESS_DENIED, context.localize("command.blacklisted"))))
                // The user is not rate limited
                // TODO Improvement: Do not acknowledge if rate limited
                .filterWhen(__ -> BooleanUtils.not(CommandProcessor.isRateLimited(context, command)))
                .flatMap(__ -> command.execute(context))
                .doOnSuccess(__ -> {
                    Telemetry.COMMAND_USAGE_COUNTER.labels(command.getName()).inc();
                    Telemetry.INTERACTING_USERS.add(context.getAuthorId().asLong());
                })
                .onErrorResume(err -> ExceptionHandler.handleCommandError(err, context)
                        .then(Mono.empty()));
    }

    private static Mono<Boolean> isRateLimited(Context context, BaseCmd cmd) {
        return Mono.justOrEmpty(cmd.getRateLimiter())
                .flatMap(ratelimiter -> {
                    final RateLimitResponse response = ratelimiter.isLimited(context.getGuildId(), context.getAuthorId());
                    if (response.shouldBeWarned()) {
                        return context.reply(Emoji.STOPWATCH, ratelimiter.formatRateLimitMessage(context.getLocale()))
                                .thenReturn(response.isLimited());
                    }
                    return Mono.just(response.isLimited());
                })
                .defaultIfEmpty(false);
    }

}
