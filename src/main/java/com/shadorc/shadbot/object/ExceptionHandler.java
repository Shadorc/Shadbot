package com.shadorc.shadbot.object;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.command.MissingPermissionException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.music.NoMusicException;
import com.shadorc.shadbot.utils.FormatUtil;
import io.netty.channel.unix.Errors;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class ExceptionHandler {

    public static final Function<String, RetryBackoffSpec> RETRY_ON_INTERNET_FAILURES =
            message -> Retry.backoff(3, Duration.ofSeconds(1))
                    .filter(err -> err instanceof PrematureCloseException
                            || err instanceof Errors.NativeIoException
                            || err instanceof TimeoutException)
                    .onRetryExhaustedThrow((spec, signal) -> new IOException(message));

    public static Mono<?> handleCommandError(Throwable err, Context context) {
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
            return ExceptionHandler.onServerAccessError(err, context);
        }
        return ExceptionHandler.onUnknown(err, context);
    }

    private static Mono<?> onCommandException(CommandException err, Context context) {
        return context.reply(Emoji.GREY_EXCLAMATION, err.getMessage());
    }

    private static Mono<?> onMissingPermissionException(MissingPermissionException err, Context context) {
        final String missingPerm = FormatUtil.capitalizeEnum(err.getPermission());
        DEFAULT_LOGGER.info("{Guild ID: {}} Missing permission: {}", context.getGuildId().asString(), missingPerm);

        return context.reply(Emoji.ACCESS_DENIED, context.localize("exception.permissions")
                .formatted(FormatUtil.capitalizeEnum(err.getPermission())));
    }

    private static Mono<?> onNoMusicException(Context context) {
        return context.reply(Emoji.MUTE, context.localize("exception.no.music"));
    }

    private static Mono<?> onServerAccessError(Throwable err, Context context) {
        final Throwable cause = err.getCause() != null ? err.getCause() : err;
        DEFAULT_LOGGER.warn("{Guild ID: {}} [{}] Server access error. {}: {}\n{}",
                context.getGuildId().asString(),
                context.getFullCommandName(),
                cause.getClass().getName(),
                cause.getMessage(),
                context.getEvent().getInteraction().getData().data());

        return context.reply(Emoji.RED_FLAG, context.localize("exception.server.access")
                .formatted(context.getFullCommandName()));
    }

    private static Mono<?> onUnknown(Throwable err, Context context) {
        DEFAULT_LOGGER.error("{Guild ID: %s} [%s] An unknown error occurred: %s\n%s"
                        .formatted(context.getGuildId().asString(),
                                context.getFullCommandName(),
                                Objects.requireNonNullElse(err.getMessage(), ""),
                                context.getEvent().getInteraction().getData().data()),
                err);

        return context.reply(Emoji.RED_FLAG, context.localize("exception.unknown")
                .formatted(context.getFullCommandName()));
    }

    public static void handleUnknownError(Throwable err) {
        DEFAULT_LOGGER.error("An unknown error occurred: %s"
                .formatted(Objects.requireNonNullElse(err.getMessage(), "")), err);
    }

}
