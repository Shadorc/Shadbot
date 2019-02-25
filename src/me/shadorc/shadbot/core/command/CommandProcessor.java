package me.shadorc.shadbot.core.command;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Predicate;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.ratelimiter.RateLimiter;
import me.shadorc.shadbot.data.database.DBGuild;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.CommandEnum;
import me.shadorc.shadbot.data.stats.enums.VariousEnum;
import me.shadorc.shadbot.listener.interceptor.MessageInterceptorManager;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.exception.ExceptionHandler;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

public class CommandProcessor {

	public static Mono<Void> processMessageEvent(MessageCreateEvent event) {
		// This is not a private channel
		if(!event.getGuildId().isPresent()) {
			return CommandProcessor.onPrivateMessage(event);
		}

		// The content is not a Webhook
		if(!event.getMessage().getContent().isPresent()) {
			return Mono.empty();
		}

		final DBGuild dbGuild = Shadbot.getDatabase().getDBGuild(event.getGuildId().get());
		final String content = event.getMessage().getContent().get();
		return Mono.justOrEmpty(event.getMember())
				// The author is not a bot
				.filter(member -> !member.isBot())
				.flatMap(member -> member.getRoles().collectList())
				// The role is allowed
				.filter(roles -> dbGuild.hasAllowedRole(roles))
				.flatMap(ignored -> event.getMessage().getChannel())
				// The channel is allowed
				.filter(channel -> dbGuild.isTextChannelAllowed(channel.getId()))
				// The message has not been intercepted
				.filterWhen(ignored -> MessageInterceptorManager.isIntercepted(event).map(Boolean.FALSE::equals))
				.map(ignored -> dbGuild.getPrefix())
				// The message starts with the correct prefix
				.filter(prefix -> content.startsWith(prefix))
				.flatMap(prefix -> CommandProcessor.executeCommand(new Context(event, prefix)));
	}

	private static Mono<Void> executeCommand(Context context) {
		final AbstractCommand command = CommandInitializer.getCommand(context.getCommandName());
		if(command == null) {
			return Mono.empty();
		}

		final Snowflake guildId = context.getGuildId();

		final Predicate<? super AbstractCommand> isRateLimited = cmd -> {
			final Optional<RateLimiter> rateLimiter = cmd.getRateLimiter();
			if(!rateLimiter.isPresent()) {
				return false;
			}

			if(rateLimiter.get().isLimitedAndWarn(context.getClient(), guildId, context.getChannelId(), context.getAuthorId())) {
				StatsManager.COMMAND_STATS.log(CommandEnum.COMMAND_LIMITED, cmd);
				return true;
			}
			return false;
		};

		return context.getPermission()
				// The author has the permission to execute this command
				.filter(userPerm -> !command.getPermission().isSuperior(userPerm))
				.switchIfEmpty(context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(
								String.format(Emoji.ACCESS_DENIED + " (**%s**) You do not have the permission to execute this command.",
										context.getUsername()), channel)
								.then(Mono.empty())))
				.flatMap(perm -> Mono.just(command))
				// The command is allowed in the guild
				.filter(cmd -> Shadbot.getDatabase().getDBGuild(guildId).isCommandAllowed(cmd))
				// The user is not rate limited
				.filter(isRateLimited.negate())
				.flatMap(cmd -> cmd.execute(context))
				.onErrorResume(err -> ExceptionHandler.handleCommandError(err, command, context))
				.doOnTerminate(() -> {
					StatsManager.COMMAND_STATS.log(CommandEnum.COMMAND_USED, command);
					StatsManager.VARIOUS_STATS.log(VariousEnum.COMMANDS_EXECUTED);
				});
	}

	private static Mono<Void> onPrivateMessage(MessageCreateEvent event) {
		StatsManager.VARIOUS_STATS.log(VariousEnum.PRIVATE_MESSAGES_RECEIVED);

		final String text = String.format("Hello !"
				+ "%nCommands only work in a server but you can see help using `%shelp`."
				+ "%nIf you have a question, a suggestion or if you just want to talk, don't hesitate to "
				+ "join my support server : %s",
				Config.DEFAULT_PREFIX, Config.SUPPORT_SERVER_URL);

		final String content = event.getMessage().getContent().orElse("");
		if(content.startsWith(Config.DEFAULT_PREFIX + "help")) {
			return CommandInitializer.getCommand("help")
					.execute(new Context(event, Config.DEFAULT_PREFIX));
		} else {
			return event.getMessage()
					.getChannel()
					.flatMapMany(channel -> channel.getMessagesBefore(Snowflake.of(Instant.now())))
					.map(Message::getContent)
					.flatMap(Mono::justOrEmpty)
					.take(50)
					.collectList()
					.filter(list -> !list.stream().anyMatch(text::equalsIgnoreCase))
					.flatMap(ignored -> event.getMessage().getChannel())
					.flatMap(channel -> DiscordUtils.sendMessage(text, channel))
					.then();
		}
	}

}
