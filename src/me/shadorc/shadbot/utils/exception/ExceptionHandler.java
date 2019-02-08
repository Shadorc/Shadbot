package me.shadorc.shadbot.utils.exception;

import discord4j.core.DiscordClient;
import discord4j.rest.http.client.ClientException;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.CommandEnum;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.exception.MissingPermissionException;
import me.shadorc.shadbot.exception.MissingPermissionException.UserType;
import me.shadorc.shadbot.exception.NoMusicException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

public class ExceptionHandler {

	public static Mono<Void> handleCommandError(Throwable err, AbstractCommand cmd, Context context) {
		if(err instanceof CommandException) {
			return ExceptionHandler.onCommandException((CommandException) err, cmd, context);
		}
		if(err instanceof MissingPermissionException) {
			return ExceptionHandler.onMissingPermissionException((MissingPermissionException) err, context);
		}
		if(err instanceof MissingArgumentException) {
			return ExceptionHandler.onMissingArgumentException(cmd, context);
		}
		if(err instanceof NoMusicException) {
			return ExceptionHandler.onNoMusicException(context);
		}
		if(ExceptionUtils.isUnavailable(err)) {
			return ExceptionHandler.onUnavailable(cmd, context);
		}
		if(ExceptionUtils.isUnreacheable(err)) {
			return ExceptionHandler.onUnreacheable(cmd, context);
		}
		return ExceptionHandler.onUnknown(err, cmd, context);
	}

	private static Mono<Void> onCommandException(CommandException err, AbstractCommand cmd, Context context) {
		return Mono.fromRunnable(() -> StatsManager.COMMAND_STATS.log(CommandEnum.COMMAND_ILLEGAL_ARG, cmd))
				.and(context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.GREY_EXCLAMATION + " (**%s**) %s",
								context.getUsername(), err.getMessage()), channel)));
	}

	private static Mono<Void> onMissingPermissionException(MissingPermissionException err, Context context) {
		final String missingPerm = StringUtils.capitalizeEnum(err.getPermission());
		if(err.getType().equals(UserType.BOT)) {
			return Mono.fromRunnable(() -> LogUtils.info("{Guild ID: %d} Missing permission: %s",
					context.getGuildId().asLong(), missingPerm))
					.and(context.getChannel()
							.flatMap(channel -> DiscordUtils.sendMessage(
									TextUtils.missingPermission(context.getUsername(), err.getPermission()), channel)));
		} else {
			return context.getChannel()
					.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.ACCESS_DENIED
							+ " (**%s**) You can't execute this command because you don't have the permission to %s.",
							context.getUsername(), String.format("**%s**", missingPerm)), channel))
					.then();
		}
	}

	private static Mono<Void> onMissingArgumentException(AbstractCommand cmd, Context context) {
		return Mono.fromRunnable(() -> StatsManager.COMMAND_STATS.log(CommandEnum.COMMAND_MISSING_ARG, cmd))
				.and(context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(
								Emoji.WHITE_FLAG + " Some arguments are missing, here is the help for this command.", cmd.getHelp(context), channel)));
	}

	private static Mono<Void> onNoMusicException(Context context) {
		return context.getChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.MUTE + " (**%s**) No currently playing music.",
						context.getUsername()), channel))
				.then();
	}

	private static Mono<Void> onUnavailable(AbstractCommand cmd, Context context) {
		return Mono.fromRunnable(() -> LogUtils.warn(context.getClient(),
				String.format("{Guild ID: %d} [%s] Service unavailable.", context.getGuildId().asLong(), cmd.getClass().getSimpleName()),
				context.getContent()))
				.and(context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.RED_FLAG + " (**%s**) Mmmh... `%s%s` is currently unavailable... "
								+ "This is not my fault, I promise ! Try again later.",
								context.getUsername(), context.getPrefix(), context.getCommandName()), channel)));
	}

	private static Mono<Void> onUnreacheable(AbstractCommand cmd, Context context) {
		return Mono.fromRunnable(() -> LogUtils.warn(context.getClient(),
				String.format("{Guild ID: %d} [%s] Service unreachable.", context.getGuildId().asLong(), cmd.getClass().getSimpleName()),
				context.getContent()))
				.and(context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.RED_FLAG + " (**%s**) Mmmh... `%s%s` takes too long to be executed... "
								+ "This is not my fault, I promise ! Try again later.",
								context.getUsername(), context.getPrefix(), context.getCommandName()), channel)));
	}

	private static Mono<Void> onUnknown(Throwable err, AbstractCommand cmd, Context context) {
		return Mono.fromRunnable(() -> LogUtils.error(context.getClient(), err,
				String.format("{Guild ID: %d} [%s] An unknown error occurred.", context.getGuildId().asLong(), cmd.getClass().getSimpleName()),
				context.getContent()))
				.and(context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(
								String.format(Emoji.RED_FLAG + " (**%s**) Sorry, something went wrong while executing `%s%s`. My developer has been warned.",
										context.getUsername(), context.getPrefix(), context.getCommandName()), channel)));
	}

	public static void handleUnknownError(DiscordClient client, Throwable err) {
		if(ExceptionUtils.isForbidden(err) || ExceptionUtils.isNotFound(err) || ExceptionUtils.isInternalServerError(err)) {
			final ClientException clientErr = (ClientException) err;
			LogUtils.info("%s: %s (URL: %s)",
					clientErr.getStatus(), clientErr.getErrorResponse().getFields().get("message").toString(), clientErr.getRequest().url());
		} else {
			LogUtils.error(client, err, "An unknown error occurred.");
		}
	}

}
