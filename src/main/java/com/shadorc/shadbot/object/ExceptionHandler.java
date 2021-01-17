package com.shadorc.shadbot.object;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.command.MissingPermissionException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.music.NoMusicException;
import com.shadorc.shadbot.utils.FormatUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class ExceptionHandler {

    public static Mono<?> handleCommandError(Throwable err, BaseCmd cmd, Context context) {
        if (err instanceof CommandException) {
            return ExceptionHandler.onCommandException((CommandException) err, context);
        }
        if (err instanceof MissingPermissionException) {
            return ExceptionHandler.onMissingPermissionException((MissingPermissionException) err, context);
        }
        if (err instanceof NoMusicException) {
            return ExceptionHandler.onNoMusicException(context);
        }
        if (err instanceof TimeoutException || err instanceof IOException) {
            return ExceptionHandler.onServerAccessError(err, cmd, context);
        }
        return ExceptionHandler.onUnknown(err, cmd, context);
    }

    private static Mono<?> onCommandException(CommandException err, Context context) {
        return context.createFollowupMessage(Emoji.GREY_EXCLAMATION + " (**%s**) %s", context.getAuthorName(), err.getMessage());
    }

    private static Mono<?> onMissingPermissionException(MissingPermissionException err, Context context) {
        final String missingPerm = FormatUtils.capitalizeEnum(err.getPermission());
        DEFAULT_LOGGER.info("{Guild ID: {}} Missing permission: {}", context.getGuildId().asString(), missingPerm);

        return context.createFollowupMessage(
                Emoji.ACCESS_DENIED + " (**%s**) I can't execute this command due to the lack of permission."
                        + "%nPlease, check my permissions and channel-specific ones to verify that **%s** is checked.",
                context.getAuthorName(), FormatUtils.capitalizeEnum(err.getPermission()));
    }

    private static Mono<?> onNoMusicException(Context context) {
        return context.createFollowupMessage(Emoji.MUTE + " (**%s**) No currently playing music.", context.getAuthorName());
    }

    private static Mono<?> onServerAccessError(Throwable err, BaseCmd cmd, Context context) {
        final Throwable cause = err.getCause() != null ? err.getCause() : err;
        DEFAULT_LOGGER.warn("{Guild ID: {}} [{}] Server access error on input '{}'. {}: {}",
                context.getGuildId().asString(), cmd.getClass().getSimpleName(), context.getEvent().getCommandName(),
                cause.getClass().getName(), cause.getMessage());

        return context.createFollowupMessage(Emoji.RED_FLAG + " (**%s**) Mmmh... The web service related to the `%s` "
                        + "command is not available right now... This is not my fault, I promise ! Try again later.",
                context.getAuthorName(), context.getCommandName());
    }

    private static Mono<?> onUnknown(Throwable err, BaseCmd cmd, Context context) {
        DEFAULT_LOGGER.error(String.format("{Guild ID: %d} [%s] An unknown error occurred (input: %s): %s",
                context.getGuildId().asLong(), cmd.getClass().getSimpleName(), context.getCommandName(),
                Objects.requireNonNullElse(err.getMessage(), "")), err);

        return context.createFollowupMessage(Emoji.RED_FLAG + " (**%s**) Sorry, something went wrong while " +
                "executing `%s`. My developer has been warned.", context.getAuthorName(), context.getCommandName());
    }

    public static void handleUnknownError(Throwable err) {
        DEFAULT_LOGGER.error(String.format("An unknown error occurred: %s",
                Objects.requireNonNullElse(err.getMessage(), "")), err);
    }

}
