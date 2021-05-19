package com.locibot.locibot.core.command;

import com.locibot.locibot.LociBot;
import com.locibot.locibot.core.ratelimiter.RateLimitResponse;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.object.ExceptionHandler;
import com.locibot.locibot.utils.ReactorUtil;
import discord4j.core.object.entity.Guild;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Mono;

public class CommandProcessor {

    public static Mono<?> processCommand(Context context) {
        if (context.isPrivate()){
            return Mono.just(CommandProcessor.executePrivateCommand(context)).log();
        }
        return Mono.just(context.getAuthor())
                // The role is allowed or the author is the guild's owner
                .filterWhen(ReactorUtil.filterWhenOrExecute(
                        member -> BooleanUtils.or(
                                member.getRoles().collectList().map(context.getDbGuild().getSettings()::hasAllowedRole),
                                member.getGuild().map(Guild::getOwnerId).map(member.getId()::equals)),
                        context.replyEphemeral(Emoji.ACCESS_DENIED, context.localize("role.not.allowed"))))
                // The channel is allowed
                .filterWhen(ReactorUtil.filterOrExecute(
                        __ -> context.getDbGuild().getSettings().isTextChannelAllowed(context.getChannelId()),
                        context.replyEphemeral(Emoji.ACCESS_DENIED, context.localize("channel.not.allowed"))))
                // Execute the command
                .flatMap(__ -> CommandProcessor.executeCommand(context));
    }

    private static Mono<?> executePrivateCommand(Context context) {
        final BaseCmd command = CommandManager.getCommand(context.getLastCommandName());
        return context.getEvent().acknowledge().thenReturn(command.execute(context));
    }

    private static Mono<?> executeCommand(Context context) {
        final BaseCmd command = CommandManager.getCommand(context.getLastCommandName());
        // The command does not exist
        if (command == null) {
            LociBot.DEFAULT_LOGGER.error("{Guild ID: {}} Command {} not found",
                    context.getGuildId().asString(), context.getFullCommandName());
            return Mono.error(new RuntimeException("Command %s not found".formatted(context.getFullCommandName())));
        }

        // The command has been disabled
        if (!command.isEnabled()) {
            return context.replyEphemeral(Emoji.ACCESS_DENIED, context.localize("command.disabled")
                    .formatted(Config.SUPPORT_SERVER_URL));
        }

        // This category is not allowed in this channel
        if (!context.getDbGuild().getSettings().isCommandAllowedInChannel(command, context.getChannelId())) {
            return context.replyEphemeral(Emoji.ACCESS_DENIED, context.localize("command.channel.not.allowed"));
        }

        // This command is not allowed to this role
        if (!context.getDbGuild().getSettings().isCommandAllowedToRole(command, context.getAuthor().getRoleIds())) {
            return context.replyEphemeral(Emoji.ACCESS_DENIED, context.localize("command.role.not.allowed"));
        }

        return context.getPermissions()
                .collectList()
                // The author has the permission to execute this command
                .filterWhen(ReactorUtil.filterOrExecute(
                        userPerms -> userPerms.contains(command.getPermission()),
                        context.replyEphemeral(Emoji.ACCESS_DENIED, context.localize("command.missing.permission"))))
                // The command is allowed in the guild
                .filterWhen(ReactorUtil.filterOrExecute(
                        __ -> context.getDbGuild().getSettings().isCommandAllowed(command),
                        context.replyEphemeral(Emoji.ACCESS_DENIED, context.localize("command.blacklisted"))))
                // The user is not rate limited
                .filterWhen(__ -> BooleanUtils.not(CommandProcessor.isRateLimited(context, command)))
                .flatMap(__ -> context.getEvent().acknowledge()
                        // Without this, BaseCmd#execute errors would be silently discarded
                        .thenReturn(__))
                .flatMap(__ -> command.execute(context))
                .doOnSuccess(__ -> Telemetry.COMMAND_USAGE_COUNTER.labels(command.getName()).inc())
                .onErrorResume(err -> ExceptionHandler.handleCommandError(err, context)
                        .then(Mono.empty()));
    }

    private static Mono<Boolean> isRateLimited(Context context, BaseCmd cmd) {
        return Mono.justOrEmpty(cmd.getRateLimiter())
                .flatMap(ratelimiter -> {
                    final RateLimitResponse response = ratelimiter.isLimited(context.getGuildId(), context.getAuthorId());
                    if (response.shouldBeWarned()) {
                        return context.replyEphemeral(Emoji.STOPWATCH, ratelimiter.formatRateLimitMessage(context.getLocale()))
                                .thenReturn(response.isLimited());
                    }
                    return Mono.just(response.isLimited());
                })
                .defaultIfEmpty(false);
    }

}
