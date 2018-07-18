package me.shadorc.shadbot.core;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import org.apache.http.HttpStatus;
import org.jsoup.HttpStatusException;

import discord4j.core.object.entity.Message;
import discord4j.rest.http.client.ClientException;
import io.netty.handler.codec.http.HttpResponseStatus;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.stats.CommandStatsManager;
import me.shadorc.shadbot.data.stats.CommandStatsManager.CommandEnum;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.exception.NoMusicException;
import me.shadorc.shadbot.utils.BotUtils;
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
		if(this.isCommandException()) {
			return this.onCommandException();
		}
		if(this.isMissingArgumentException()) {
			return this.onMissingArgumentException();
		}
		if(this.isNoMusicException()) {
			return this.onNoMusicException();
		}
		if(this.isUnavailable()) {
			return this.onUnavailable();
		}
		if(this.isUnreacheable()) {
			return this.onUnreacheable();
		}
		if(this.isForbidden()) {
			return this.onForbidden();
		}
		return this.onUnknown();
	}

	private boolean isCommandException() {
		return err instanceof CommandException;
	}

	private boolean isMissingArgumentException() {
		return err instanceof MissingArgumentException;
	}

	private boolean isNoMusicException() {
		return err instanceof NoMusicException;
	}

	private boolean isUnavailable() {
		return err instanceof ConnectException
				|| err instanceof HttpStatusException && HttpStatusException.class.cast(err).getStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE;
	}

	private boolean isUnreacheable() {
		return err instanceof SocketTimeoutException;
	}

	public boolean isForbidden() {
		return err instanceof ClientException
				&& ClientException.class.cast(err).getStatus().equals(HttpResponseStatus.FORBIDDEN);
	}

	public boolean isNotForbidden() {
		return !this.isForbidden();
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
				String.format(Emoji.RED_FLAG + " (**%s**) Mmmh... `%s%s` is currently unavailable... This is not my fault, I promise ! Try again later.",
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
		// TODO Auto-generated method stub
		return Mono.empty();
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
