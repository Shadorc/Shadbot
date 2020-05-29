package com.shadorc.shadbot.utils;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.command.MissingArgumentException;
import com.shadorc.shadbot.command.MissingPermissionException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.music.NoMusicException;
import com.shadorc.shadbot.object.Emoji;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class ExceptionHandler {

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
        DEFAULT_LOGGER.info("{Guild ID: {}} Missing permission: {}", context.getGuildId().asLong(), missingPerm);
        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(
                        String.format(Emoji.ACCESS_DENIED + " (**%s**) I can't execute this command due to the lack of "
                                        + "permission.%nPlease, check my permissions and channel-specific "
                                        + "ones to verify that %s is checked.", context.getUsername(),
                                String.format("**%s**", StringUtils.capitalizeEnum(err.getPermission()))), channel))
                .then();
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
                .flatMap(channel -> DiscordUtils.sendMessage(
                        String.format(Emoji.MUTE + " (**%s**) No currently playing music.",
                                context.getUsername()), channel))
                .then();
    }

    private static Mono<Void> onServerAccessError(Throwable err, BaseCmd cmd, Context context) {
        final Throwable cause = err.getCause() != null ? err.getCause() : err;
        DEFAULT_LOGGER.warn("{Guild ID: {}} [{}] Server access error on input '{}'. {}: {}",
                context.getGuildId().asLong(), cmd.getClass().getSimpleName(), context.getContent(),
                cause.getClass().getName(), cause.getMessage());
        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(
                        String.format(Emoji.RED_FLAG + " (**%s**) Mmmh... The web service related to the `%s%s` "
                                        + "command is not available right now... This is not my fault, I promise !"
                                        + " Try again later.",
                                context.getUsername(), context.getPrefix(), context.getCommandName()), channel))
                .then();
    }

    private static Mono<Void> onUnknown(Throwable err, BaseCmd cmd, Context context) {
        DEFAULT_LOGGER.error(String.format("{Guild ID: %d} [%s] An unknown error occurred (input: %s): %s",
                context.getGuildId().asLong(), cmd.getClass().getSimpleName(), context.getContent(),
                Objects.requireNonNullElse(err.getMessage(), "")), err);

        return context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(
                        String.format(Emoji.RED_FLAG + " (**%s**) Sorry, something went wrong while " +
                                        "executing `%s%s`. My developer has been warned.",
                                context.getUsername(), context.getPrefix(), context.getCommandName()), channel))
                .then();
    }

    public static void handleUnknownError(Throwable err) {
        DEFAULT_LOGGER.error(String.format("An unknown error occurred: %s",
                Objects.requireNonNullElse(err.getMessage(), "")), err);
    }

}
