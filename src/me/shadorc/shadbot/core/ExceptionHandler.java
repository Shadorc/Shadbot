package me.shadorc.shadbot.core;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import org.apache.http.HttpStatus;
import org.jsoup.HttpStatusException;

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

public class ExceptionHandler {

	private final Throwable err;
	private final AbstractCommand command;
	private final Context context;

	public ExceptionHandler(Throwable err, AbstractCommand command, Context context) {
		this.err = err;
		this.command = command;
		this.context = context;
	}

	public void handle() {
		if(this.isCommandException()) {
			this.onCommandException();
		} else if(this.isMissingArgumentException()) {
			this.onMissingArgumentException();
		} else if(this.isNoMusicException()) {
			this.onNoMusicException();
		} else if(this.isUnavailable()) {
			this.onUnavailable();
		} else if(this.isUnreacheable()) {
			this.onUnreacheable();
		} else if(this.isUnknown()) {
			this.onUnknown();
		}
	}

	private boolean isCommandException() {
		return err.getClass().isInstance(CommandException.class);
	}

	private boolean isMissingArgumentException() {
		return err.getClass().isInstance(MissingArgumentException.class);
	}

	private boolean isNoMusicException() {
		return err.getClass().isInstance(NoMusicException.class);
	}

	private boolean isUnavailable() {
		return err.getClass().isInstance(ConnectException.class)
				|| err.getClass().isInstance(HttpStatusException.class) && HttpStatusException.class.cast(err).getStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE;
	}

	private boolean isUnreacheable() {
		return err.getClass().isInstance(SocketTimeoutException.class);
	}

	public static boolean isForbidden(Throwable err) {
		return err.getClass().isInstance(ClientException.class)
				&& ClientException.class.cast(err).getStatus().equals(HttpResponseStatus.FORBIDDEN);
	}

	public static boolean isNotForbidden(Throwable err) {
		return !ExceptionHandler.isForbidden(err);
	}

	private boolean isUnknown() {
		return !this.isCommandException()
				&& !this.isNoMusicException()
				&& !this.isMissingArgumentException()
				&& !this.isUnavailable()
				&& !this.isUnreacheable()
				&& !ExceptionHandler.isForbidden(err);
	}

	private void onCommandException() {
		context.getAuthorName()
				.flatMap(username -> BotUtils.sendMessage(
						String.format(Emoji.GREY_EXCLAMATION + " (**%s**) %s", username, err.getMessage()), context.getChannel()))
				.subscribe();
		CommandStatsManager.log(CommandEnum.COMMAND_ILLEGAL_ARG, command);
	}

	private void onMissingArgumentException() {
		command.getHelp(context)
				.flatMap(embed -> BotUtils.sendMessage(TextUtils.MISSING_ARG, embed, context.getChannel()))
				.subscribe();
		CommandStatsManager.log(CommandEnum.COMMAND_MISSING_ARG, command);
	}

	private void onNoMusicException() {
		BotUtils.sendMessage(TextUtils.NO_PLAYING_MUSIC, context.getChannel()).subscribe();
	}

	private void onUnavailable() {
		context.getAuthorName()
				.flatMap(username -> BotUtils.sendMessage(
						String.format(Emoji.RED_FLAG + " (**%s**) Mmmh... `%s%s` is currently unavailable... This is not my fault, I promise ! Try again later.",
								username, context.getPrefix(), context.getCommandName()), context.getChannel()))
				.subscribe();
		LogUtils.warn(context.getClient(),
				String.format("[%s] Service unavailable.", command.getClass().getSimpleName()),
				context.getContent());
	}

	private void onUnreacheable() {
		context.getAuthorName()
				.flatMap(username -> BotUtils.sendMessage(
						String.format(Emoji.RED_FLAG + " (**%s**) Mmmh... `%s%s` takes too long to be executed... This is not my fault, I promise ! Try again later.",
								username, context.getPrefix(), context.getCommandName()), context.getChannel()))
				.subscribe();
		LogUtils.warn(context.getClient(),
				String.format("[%s] Service unreachable.", command.getClass().getSimpleName()),
				context.getContent());
	}

	private void onUnknown() {
		context.getAuthorName()
				.flatMap(username -> BotUtils.sendMessage(
						String.format(Emoji.RED_FLAG + " (**%s**) Sorry, something went wrong while executing `%s%s`. My developer has been warned.",
								username, context.getPrefix(), context.getCommandName()), context.getChannel()))
				.subscribe();
		LogUtils.error(context.getClient(),
				err,
				String.format("[%s] An unknown error occurred.", command.getClass().getSimpleName()),
				context.getContent());
	}

}
