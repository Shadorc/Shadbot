package me.shadorc.shadbot.core;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.jsoup.HttpStatusException;

import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Permission;
import discord4j.rest.http.client.ClientException;
import io.netty.handler.codec.http.HttpResponseStatus;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.stats.CommandStatsManager;
import me.shadorc.shadbot.data.stats.CommandStatsManager.CommandEnum;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.exception.MissingPermissionException;
import me.shadorc.shadbot.exception.NoMusicException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Mono;

public class ExceptionHandler {

	private final Throwable err;
	private final AbstractCommand command;
	private final Context context;

	public ExceptionHandler(Throwable err, AbstractCommand command, Context context) {
		this.err = err;
		this.command = command;
		this.context = context;
	}

	public Mono<Message> handle() {
		if(ExceptionHandler.isCommandException(err)) {
			return this.onCommandException();
		}
		if(ExceptionHandler.isMissingArgumentException(err)) {
			return this.onMissingArgumentException();
		}
		if(ExceptionHandler.isNoMusicException(err)) {
			return this.onNoMusicException();
		}
		if(ExceptionHandler.isUnavailable(err)) {
			return this.onUnavailable();
		}
		if(ExceptionHandler.isUnreacheable(err)) {
			return this.onUnreacheable();
		}
		if(ExceptionHandler.isForbidden(err)) {
			return this.onForbidden();
		}
		return this.onUnknown();
	}

	public static boolean isCommandException(Throwable err) {
		return err instanceof CommandException;
	}

	public static boolean isMissingArgumentException(Throwable err) {
		return err instanceof MissingArgumentException;
	}

	public static boolean isNoMusicException(Throwable err) {
		return err instanceof NoMusicException;
	}

	public static boolean isUnavailable(Throwable err) {
		return err instanceof ConnectException
				|| err instanceof HttpStatusException && HttpStatusException.class.cast(err).getStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE;
	}

	public static boolean isUnreacheable(Throwable err) {
		return err instanceof SocketTimeoutException;
	}

	public static boolean isForbidden(Throwable err) {
		return err instanceof MissingPermissionException
				|| err instanceof ClientException
						&& ClientException.class.cast(err).getStatus().equals(HttpResponseStatus.FORBIDDEN);
	}

	public static boolean isNotFound(Throwable err) {
		return err instanceof ClientException
				&& ClientException.class.cast(err).getStatus().equals(HttpResponseStatus.NOT_FOUND);
	}

	private Mono<Message> onCommandException() {
		CommandStatsManager.log(CommandEnum.COMMAND_ILLEGAL_ARG, command);
		return BotUtils.sendMessage(String.format(Emoji.GREY_EXCLAMATION + " (**%s**) %s",
				context.getUsername(), err.getMessage()), context.getChannel());
	}

	private Mono<Message> onMissingArgumentException() {
		CommandStatsManager.log(CommandEnum.COMMAND_MISSING_ARG, command);
		return command.getHelp(context)
				.flatMap(embed -> BotUtils.sendMessage(TextUtils.MISSING_ARG, embed, context.getChannel()));
	}

	private Mono<Message> onNoMusicException() {
		return BotUtils.sendMessage(TextUtils.NO_PLAYING_MUSIC, context.getChannel());
	}

	private Mono<Message> onUnavailable() {
		LogUtils.warn(context.getClient(),
				String.format("[%s] Service unavailable.", command.getClass().getSimpleName()),
				context.getContent());
		return BotUtils.sendMessage(
				String.format(Emoji.RED_FLAG + " (**%s**) Mmmh... `%s%s` is currently unavailable... "
						+ "This is not my fault, I promise ! Try again later.",
						context.getUsername(), context.getPrefix(), context.getCommandName()), context.getChannel());
	}

	private Mono<Message> onUnreacheable() {
		LogUtils.warn(context.getClient(),
				String.format("[%s] Service unreachable.", command.getClass().getSimpleName()),
				context.getContent());
		return BotUtils.sendMessage(
				String.format(Emoji.RED_FLAG + " (**%s**) Mmmh... `%s%s` takes too long to be executed... This is not my fault, I promise ! Try again later.",
						context.getUsername(), context.getPrefix(), context.getCommandName()), context.getChannel());
	}

	private Mono<Message> onForbidden() {
		final List<Permission> permissions = new ArrayList<>(command.getPermissions());
		if(permissions.isEmpty()) {
			permissions.add(Permission.EMBED_LINKS);
		}

		final List<String> permissionsStr = permissions.stream()
				.map(FormatUtils::formatPermission)
				.collect(Collectors.toList());

		return BotUtils.sendMessage(String.format(Emoji.ACCESS_DENIED + " I can't execute this command due to the lack of permission."
				+ "%nPlease, check my permissions and channel-specific ones to verify that %s %s checked.",
				FormatUtils.format(permissionsStr, str -> String.format("**%s**", str), " and "),
				permissionsStr.size() > 1 ? "are" : "is"), context.getChannel())
				.doOnSuccess(message -> LogUtils.infof("{Guild ID: %d} Missing permission(s): %s",
						context.getGuildId().asLong(), String.join(", ", permissionsStr)))
				.doOnError(ExceptionHandler::isForbidden, err -> LogUtils.cannotSpeak(this.getClass(), context.getGuildId()));
	}

	private Mono<Message> onUnknown() {
		LogUtils.error(context.getClient(),
				err,
				String.format("[%s] An unknown error occurred.", command.getClass().getSimpleName()),
				context.getContent());
		return BotUtils.sendMessage(
				String.format(Emoji.RED_FLAG + " (**%s**) Sorry, something went wrong while executing `%s%s`. My developer has been warned.",
						context.getUsername(), context.getPrefix(), context.getCommandName()), context.getChannel());
	}

}
