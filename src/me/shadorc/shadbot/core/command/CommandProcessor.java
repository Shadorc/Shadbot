package me.shadorc.shadbot.core.command;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.lang3.BooleanUtils;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Channel.Type;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.ExceptionHandler;
import me.shadorc.shadbot.core.ratelimiter.RateLimiter;
import me.shadorc.shadbot.data.database.DatabaseManager;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.CommandEnum;
import me.shadorc.shadbot.data.stats.enums.VariousEnum;
import me.shadorc.shadbot.listener.interceptor.MessageInterceptorManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

public class CommandProcessor {

	public static void processMessageEvent(MessageCreateEvent event) {
		final Optional<Snowflake> guildId = event.getGuildId();

		final Function<MessageChannel, Mono<Boolean>> isNotDm = channel -> {
			if(channel.getType().equals(Type.GUILD_TEXT)) {
				return Mono.just(true);
			}
			return CommandProcessor.onPrivateMessage(event).thenReturn(false);
		};

		final Function<MessageChannel, Mono<Boolean>> canSendMessages = channel -> {
			return DiscordUtils.hasPermission(event.getMessage().getChannel(), event.getClient().getSelfId().get(), Permission.SEND_MESSAGES)
					.map(canSend -> {
						if(!canSend) {
							LogUtils.infof("{Guild ID: %d} Shadbot cannot send messages.", guildId.get().asLong());
						}
						return canSend;
					});
		};

		Mono.just(event.getMessage())
				// The content is not a Webhook
				.filter(message -> message.getContent().isPresent())
				.flatMap(Message::getAuthor)
				// The author is not a bot
				.filter(author -> !author.isBot())
				.flatMap(author -> event.getMessage().getChannel())
				// This is not a private message...
				.filterWhen(isNotDm)
				.filterWhen(canSendMessages)
				// The channel is allowed
				.filter(channel -> BotUtils.isTextChannelAllowed(guildId.get(), channel.getId()))
				.flatMap(channel -> event.getMember().get().getRoles().collectList())
				// The role is allowed
				.filter(roles -> BotUtils.hasAllowedRole(guildId.get(), roles))
				// The message has not been intercepted
				.filterWhen(roles -> MessageInterceptorManager.isIntercepted(event).map(BooleanUtils::negate))
				.map(roles -> DatabaseManager.getDBGuild(guildId.get()).getPrefix())
				// The message starts with the correct prefix
				.filter(prefix -> event.getMessage().getContent().get().startsWith(prefix))
				.flatMap(prefix -> CommandProcessor.executeCommand(new Context(event, prefix)))
				.subscribe();
	}

	public static Mono<Void> executeCommand(Context context) {
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
				.switchIfEmpty(BotUtils.sendMessage(
						String.format(Emoji.ACCESS_DENIED + " (**%s**) You do not have the permission to execute this command.",
								context.getUsername()), context.getChannel())
						.then(Mono.empty()))
				.flatMap(perm -> Mono.just(command))
				// The command is allowed in the guild
				.filter(cmd -> BotUtils.isCommandAllowed(guildId, cmd))
				// The user is not rate limited
				.filter(isRateLimited.negate())
				.flatMap(cmd -> cmd.execute(context))
				.onErrorResume(err -> new ExceptionHandler(err, command, context).handle().then())
				.doOnSuccess(perm -> {
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
					.flatMap(list -> {
						if(list.stream().anyMatch(text::equalsIgnoreCase)) {
							return Mono.empty();
						}
						return BotUtils.sendMessage(text, event.getMessage().getChannel());
					})
					.then();
		}
	}

}
