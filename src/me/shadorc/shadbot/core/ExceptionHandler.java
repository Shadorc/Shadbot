package me.shadorc.shadbot.core;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;

import org.apache.http.HttpStatus;
import org.jsoup.HttpStatusException;

import discord4j.core.object.entity.Message;
import discord4j.rest.http.client.ClientException;
import io.netty.handler.codec.http.HttpResponseStatus;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.CommandEnum;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.exception.MissingPermissionException;
import me.shadorc.shadbot.exception.MissingPermissionException.Type;
import me.shadorc.shadbot.exception.NoMusicException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;
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
		if(ExceptionHandler.isCommandException(this.err)) {
			return this.onCommandException();
		}
		if(ExceptionHandler.isMissingArgumentException(this.err)) {
			return this.onMissingArgumentException();
		}
		if(ExceptionHandler.isNoMusicException(this.err)) {
			return this.onNoMusicException();
		}
		if(ExceptionHandler.isUnavailable(this.err)) {
			return this.onUnavailable();
		}
		if(ExceptionHandler.isUnreacheable(this.err)) {
			return this.onUnreacheable();
		}
		if(ExceptionHandler.isMissingPermission(this.err)) {
			return this.onMissingPermissionException();
		}
		if(ExceptionHandler.isForbidden(this.err)) {
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
		return err instanceof NoRouteToHostException || err instanceof SocketTimeoutException;
	}

	public static boolean isMissingPermission(Throwable err) {
		return err instanceof MissingPermissionException;
	}

	public static boolean isForbidden(Throwable err) {
		return err instanceof ClientException
				&& ClientException.class.cast(err).getStatus().equals(HttpResponseStatus.FORBIDDEN);
	}

	public static boolean isNotFound(Throwable err) {
		return err instanceof ClientException
				&& ClientException.class.cast(err).getStatus().equals(HttpResponseStatus.NOT_FOUND);
	}

	private Mono<Message> onCommandException() {
		StatsManager.COMMAND_STATS.log(CommandEnum.COMMAND_ILLEGAL_ARG, this.command);
		return BotUtils.sendMessage(String.format(Emoji.GREY_EXCLAMATION + " (**%s**) %s",
				this.context.getUsername(), this.err.getMessage()), this.context.getChannel());
	}

	private Mono<Message> onMissingArgumentException() {
		StatsManager.COMMAND_STATS.log(CommandEnum.COMMAND_MISSING_ARG, this.command);
		return this.command.getHelp(this.context)
				.flatMap(embed -> BotUtils.sendMessage(TextUtils.MISSING_ARG, embed, this.context.getChannel()));
	}

	private Mono<Message> onNoMusicException() {
		return BotUtils.sendMessage(String.format(Emoji.MUTE + " (**%s**) No currently playing music.",
				this.context.getUsername()), this.context.getChannel());
	}

	private Mono<Message> onUnavailable() {
		LogUtils.warn(this.context.getClient(),
				String.format("[%s] Service unavailable.", this.command.getClass().getSimpleName()),
				this.context.getContent());
		return BotUtils.sendMessage(String.format(Emoji.RED_FLAG + " (**%s**) Mmmh... `%s%s` is currently unavailable... "
				+ "This is not my fault, I promise ! Try again later.",
				this.context.getUsername(), this.context.getPrefix(), this.context.getCommandName()), this.context.getChannel());
	}

	private Mono<Message> onUnreacheable() {
		LogUtils.warn(this.context.getClient(),
				String.format("[%s] Service unreachable.", this.command.getClass().getSimpleName()),
				this.context.getContent());
		return BotUtils.sendMessage(String.format(Emoji.RED_FLAG + " (**%s**) Mmmh... `%s%s` takes too long to be executed... "
				+ "This is not my fault, I promise ! Try again later.",
				this.context.getUsername(), this.context.getPrefix(), this.context.getCommandName()), this.context.getChannel());
	}

	private Mono<Message> onMissingPermissionException() {
		final MissingPermissionException exception = (MissingPermissionException) this.err;
		final String missingPerm = StringUtils.capitalizeEnum(exception.getPermission());
		if(exception.getType().equals(Type.BOT)) {
			return BotUtils.sendMessage(String.format(Emoji.ACCESS_DENIED
					+ " (**%s**) I can't execute this command due to the lack of permission."
					+ "%nPlease, check my permissions and channel-specific ones to verify that %s is checked.",
					context.getUsername(), String.format("**%s**", missingPerm)), context.getChannel())
					.doOnSuccess(message -> LogUtils.infof("{Guild ID: %d} Missing permission: %s",
							this.context.getGuildId().asLong(), missingPerm));
		} else {
			return BotUtils.sendMessage(String.format(Emoji.ACCESS_DENIED
					+ " (**%s**) You can't execute this command because you don't have the permission to %s.",
					context.getUsername(), String.format("**%s**", missingPerm)), context.getChannel());
		}
	}

	private Mono<Message> onForbidden() {
		return BotUtils.sendMessage(String.format(Emoji.ACCESS_DENIED
				+ " (**%s**) I can't execute this command due to an unknown lack of permission.",
				context.getUsername()), context.getChannel())
				.doOnSuccess(message -> LogUtils.infof("{Guild ID: %d} Missing permission: Unknown",
						this.context.getGuildId().asLong()));
	}

	private Mono<Message> onUnknown() {
		LogUtils.error(this.context.getClient(),
				this.err,
				String.format("[%s] An unknown error occurred.", this.command.getClass().getSimpleName()),
				this.context.getContent());
		return BotUtils.sendMessage(
				String.format(Emoji.RED_FLAG + " (**%s**) Sorry, something went wrong while executing `%s%s`. My developer has been warned.",
						this.context.getUsername(), this.context.getPrefix(), this.context.getCommandName()), this.context.getChannel());
	}

}
