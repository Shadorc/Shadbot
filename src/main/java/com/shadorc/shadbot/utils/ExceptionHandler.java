package com.shadorc.shadbot.utils;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.exception.MissingArgumentException;
import com.shadorc.shadbot.exception.MissingPermissionException;
import com.shadorc.shadbot.exception.NoMusicException;
import com.shadorc.shadbot.object.Emoji;
import discord4j.core.GatewayDiscordClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public final class ExceptionHandler {

    public static Mono<Void> handleCommandError(Throwable err, BaseCmd cmd, Context context) {
        if (err instanceof CommandException) {
            return ExceptionHandler.onCommandException((CommandException) err, context);
        }
        if (err instanceof MissingPermissionException) {
            return ExceptionHandler.onMissingPermissionException((MissingPermissionException) err, context);
        }
        if (err instanceof MissingArgumentException) {
            return ExceptionHandler.onMissingArgumentException(cmd, context);
        }
        if (err instanceof NoMusicException) {
            return ExceptionHandler.onNoMusicException(context);
        }
        if (err instanceof TimeoutException || err instanceof IOException) {
            return ExceptionHandler.onServerAccessError(err, cmd, context);
        }
        return ExceptionHandler.onUnknown(err, cmd, context);
    }

    private static Mono<Void> onCommandException(CommandException err, Context context) {
        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.GREY_EXCLAMATION + " (**%s**) %s",
                        context.getUsername(), err.getMessage()), channel))
                .then();
    }

    private static Mono<Void> onMissingPermissionException(MissingPermissionException err, Context context) {
        final String missingPerm = StringUtils.capitalizeEnum(err.getPermission());
        return Mono.fromRunnable(() -> LogUtils.info("{Guild ID: %d} Missing permission: %s",
                context.getGuildId().asLong(), missingPerm))
                .and(context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(
                                TextUtils.missingPermission(context.getUsername(), err.getPermission()), channel)));
    }

    private static Mono<Void> onMissingArgumentException(BaseCmd cmd, Context context) {
        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(
                        Emoji.WHITE_FLAG + " Some arguments are missing, here is the help for this command.",
                        cmd.getHelp(context), channel))
                .then();
    }

    private static Mono<Void> onNoMusicException(Context context) {
        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.MUTE + " (**%s**) No currently playing music.",
                        context.getUsername()), channel))
                .then();
    }

    private static Mono<Void> onServerAccessError(Throwable err, BaseCmd cmd, Context context) {
        final Throwable cause = err.getCause() != null ? err.getCause() : err;
        return Mono.fromRunnable(() -> LogUtils.warn(context.getClient(),
                String.format("{Guild ID: %d} [%s] Server access error on input '%s'. %s: %s",
                        context.getGuildId().asLong(), cmd.getClass().getSimpleName(), context.getContent(),
                        cause.getClass().getName(), cause.getMessage())))
                .and(context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(String.format(
                                Emoji.RED_FLAG + " (**%s**) Mmmh... The web service related to the `%s%s` command is not available right now... "
                                        + "This is not my fault, I promise ! Try again later.",
                                context.getUsername(), context.getPrefix(), context.getCommandName()), channel)));
    }

    private static Mono<Void> onUnknown(Throwable err, BaseCmd cmd, Context context) {
        return Mono.fromRunnable(() -> LogUtils.error(context.getClient(), err,
                String.format("{Guild ID: %d} [%s] An unknown error occurred.", context.getGuildId().asLong(), cmd.getClass().getSimpleName()),
                context.getContent()))
                .and(context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(
                                String.format(Emoji.RED_FLAG + " (**%s**) Sorry, something went wrong while executing `%s%s`. My developer has been warned.",
                                        context.getUsername(), context.getPrefix(), context.getCommandName()), channel)));
    }

    public static void handleUnknownError(GatewayDiscordClient client, Throwable err) {
        LogUtils.error(client, err, "An unknown error occurred.");
    }

}
