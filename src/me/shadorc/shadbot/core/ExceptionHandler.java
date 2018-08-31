package me.shadorc.shadbot.core;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
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
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.CommandEnum;
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

	private Mono<Message> onForbidden() {
		final List<Permission> permissions = new ArrayList<>(this.command.getPermissions());
		if(permissions.isEmpty()) {
			permissions.add(Permission.EMBED_LINKS);
		}

		final List<String> permissionsStr = permissions.stream()
				.map(FormatUtils::formatPermission)
				.collect(Collectors.toList());

		return BotUtils.sendMessage(String.format(Emoji.ACCESS_DENIED + " (**%s**) I can't execute this command due to the lack of permission."
				+ "%nPlease, check my permissions and channel-specific ones to verify that %s %s checked.",
				this.context.getUsername(),
				FormatUtils.format(permissionsStr, str -> String.format("**%s**", str), " and "),
				permissionsStr.size() > 1 ? "are" : "is"), this.context.getChannel())
				.doOnSuccess(message -> LogUtils.infof("{Guild ID: %d} Missing permission(s): %s",
						this.context.getGuildId().asLong(), String.join(", ", permissionsStr)))
				.doOnError(ExceptionHandler::isForbidden, err -> LogUtils.cannotSpeak(this.getClass(), this.context.getGuildId()));
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
