package me.shadorc.shadbot.core.exception;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Message;
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
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

public class ExceptionHandler {

	public static Mono<Message> handleCommandError(Throwable err, AbstractCommand cmd, Context context) {
		if(err instanceof CommandException) {
			return ExceptionHandler.onCommandException((CommandException) err, cmd, context);
		}
		if(err instanceof MissingPermissionException) {
			return ExceptionHandler.onMissingPermissionException((MissingPermissionException) err, cmd, context);
		}
		if(err instanceof MissingArgumentException) {
			return ExceptionHandler.onMissingArgumentException(cmd, context);
		}
		if(err instanceof NoMusicException) {
			return ExceptionHandler.onNoMusicException(cmd, context);
		}
		if(ExceptionUtils.isUnavailable(err)) {
			return ExceptionHandler.onUnavailable(cmd, context);
		}
		if(ExceptionUtils.isUnreacheable(err)) {
			return ExceptionHandler.onUnreacheable(cmd, context);
		}
		return ExceptionHandler.onUnknown(err, cmd, context);
	}

	private static Mono<Message> onCommandException(CommandException err, AbstractCommand cmd, Context context) {
		return Mono.fromRunnable(() -> StatsManager.COMMAND_STATS.log(CommandEnum.COMMAND_ILLEGAL_ARG, cmd))
				.then(context.getChannel())
				.flatMap(channel -> BotUtils.sendMessage(String.format(Emoji.GREY_EXCLAMATION + " (**%s**) %s",
						context.getUsername(), err.getMessage()), channel));
	}

	private static Mono<Message> onMissingPermissionException(MissingPermissionException err, AbstractCommand cmd, Context context) {
		final String missingPerm = StringUtils.capitalizeEnum(err.getPermission());
		if(err.getType().equals(UserType.BOT)) {
			return context.getChannel()
					.flatMap(channel -> BotUtils.sendMessage(
							TextUtils.missingPermission(context.getUsername(), err.getPermission()), channel))
					.doOnSuccess(message -> LogUtils.info("{Guild ID: %d} Missing permission: %s",
							context.getGuildId().asLong(), missingPerm));
		} else {
			return context.getChannel()
					.flatMap(channel -> BotUtils.sendMessage(String.format(Emoji.ACCESS_DENIED
							+ " (**%s**) You can't execute this command because you don't have the permission to %s.",
							context.getUsername(), String.format("**%s**", missingPerm)), channel));
		}
	}

	private static Mono<Message> onMissingArgumentException(AbstractCommand cmd, Context context) {
		return Mono.fromRunnable(() -> StatsManager.COMMAND_STATS.log(CommandEnum.COMMAND_MISSING_ARG, cmd))
				.then(Mono.zip(cmd.getHelp(context), context.getChannel()))
				.flatMap(tuple -> BotUtils.sendMessage(
						Emoji.WHITE_FLAG + " Some arguments are missing, here is the help for this command.", tuple.getT1(), tuple.getT2()));
	}

	private static Mono<Message> onNoMusicException(AbstractCommand cmd, Context context) {
		return context.getChannel()
				.flatMap(channel -> BotUtils.sendMessage(String.format(Emoji.MUTE + " (**%s**) No currently playing music.",
						context.getUsername()), channel));
	}

	private static Mono<Message> onUnavailable(AbstractCommand cmd, Context context) {
		return Mono.fromRunnable(() -> LogUtils.warn(context.getClient(),
				String.format("[%s] Service unavailable.", cmd.getClass().getSimpleName()),
				context.getContent()))
				.then(context.getChannel())
				.flatMap(channel -> BotUtils.sendMessage(String.format(Emoji.RED_FLAG + " (**%s**) Mmmh... `%s%s` is currently unavailable... "
						+ "This is not my fault, I promise ! Try again later.",
						context.getUsername(), context.getPrefix(), context.getCommandName()), channel));
	}

	private static Mono<Message> onUnreacheable(AbstractCommand cmd, Context context) {
		return Mono.fromRunnable(() -> LogUtils.warn(context.getClient(),
				String.format("[%s] Service unreachable.", cmd.getClass().getSimpleName()),
				context.getContent()))
				.then(context.getChannel())
				.flatMap(channel -> BotUtils.sendMessage(String.format(Emoji.RED_FLAG + " (**%s**) Mmmh... `%s%s` takes too long to be executed... "
						+ "This is not my fault, I promise ! Try again later.",
						context.getUsername(), context.getPrefix(), context.getCommandName()), channel));
	}

	private static Mono<Message> onUnknown(Throwable err, AbstractCommand cmd, Context context) {
		return Mono.fromRunnable(() -> LogUtils.error(context.getClient(), err, String.format("[%s] An unknown error occurred.", cmd.getClass().getSimpleName()),
				context.getContent()))
				.then(context.getChannel())
				.flatMap(channel -> BotUtils.sendMessage(
						String.format(Emoji.RED_FLAG + " (**%s**) Sorry, something went wrong while executing `%s%s`. My developer has been warned.",
								context.getUsername(), context.getPrefix(), context.getCommandName()), channel));
	}

	public static Mono<Message> handleUnknownError(Throwable err, DiscordClient client) {
		if(ExceptionUtils.isForbidden(err)) {
			return ExceptionHandler.onForbidden((ClientException) err);
		}
		if(ExceptionUtils.isNotFound(err)) {
			return ExceptionHandler.onNotFound((ClientException) err);
		}
		return ExceptionHandler.onUnknown(client, (ClientException) err);
	}

	private static Mono<Message> onForbidden(ClientException err) {
		return Mono.fromRunnable(() -> LogUtils.info("%d %s: %s", err.getStatus().code(), err.getStatus().reasonPhrase(),
				err.getErrorResponse().getFields().get("message").toString()));
	}

	private static Mono<Message> onNotFound(ClientException err) {
		return Mono.fromRunnable(() -> LogUtils.info("%d %s: %s", err.getStatus().code(), err.getStatus().reasonPhrase(),
				err.getErrorResponse().getFields().get("message").toString()));
	}

	private static Mono<Message> onUnknown(DiscordClient client, ClientException err) {
		return Mono.fromRunnable(() -> LogUtils.error(client, err, "An unknown error occurred."));
	}

}
