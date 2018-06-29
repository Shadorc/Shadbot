package me.shadorc.shadbot.listener;

import java.util.Collections;
import java.util.Optional;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Channel.Type;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.CommandManager;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.data.stats.VariousStatsManager;
import me.shadorc.shadbot.data.stats.VariousStatsManager.VariousEnum;
import me.shadorc.shadbot.message.MessageManager;
import me.shadorc.shadbot.utils.BotUtils;
import reactor.core.publisher.Mono;

public class MessageListener {

	public static void onMessageCreate(MessageCreateEvent event) {
		VariousStatsManager.log(VariousEnum.MESSAGES_RECEIVED);

		final Optional<Snowflake> guildId = event.getGuildId();

		Mono.just(event.getMessage())
				// The content is not a Webhook
				.filter(message -> message.getContent().isPresent() && message.getAuthorId().isPresent())
				.flatMap(Message::getAuthor)
				// the author is not a bot
				.filter(author -> !author.isBot())
				.flatMap(author -> event.getMessage().getChannel())
				// This is not a private message...
				.filter(channel -> !channel.getType().equals(Type.DM))
				// ... else switch to #onPrivateMessage
				.switchIfEmpty(Mono.fromRunnable(() -> MessageListener.onPrivateMessage(event)))
				// The channel is allowed
				.filter(channel -> BotUtils.isChannelAllowed(guildId.get(), channel.getId()))
				.flatMapMany(channel -> event.getMember().get().getRoles().buffer())
				.defaultIfEmpty(Collections.emptyList())
				.single()
				// The role is allowed
				.filter(roles -> BotUtils.hasAllowedRole(guildId.get(), roles))
				// The message has not been intercepted
				.filter(roles -> !MessageManager.intercept(guildId.get(), event.getMessage()))
				.map(roles -> Database.getDBGuild(guildId.get()).getPrefix())
				// The message starts with the correct prefix
				.filter(prefix -> event.getMessage().getContent().get().startsWith(prefix))
				.doOnSuccess(prefix -> CommandManager.execute(new Context(event, prefix)))
				.subscribe();
	}

	private static Mono<Void> onPrivateMessage(MessageCreateEvent event) {
		VariousStatsManager.log(VariousEnum.PRIVATE_MESSAGES_RECEIVED);

		String msgContent = event.getMessage().getContent().get();
		if(msgContent.startsWith(Config.DEFAULT_PREFIX + "help")) {
			CommandManager.getCommand("help").execute(new Context(event, Config.DEFAULT_PREFIX));
			return Mono.empty();
		}

		final String text = String.format("Hello !"
				+ "%nCommands only work in a server but you can see help using `%shelp`."
				+ "%nIf you have a question, a suggestion or if you just want to talk, don't hesitate to "
				+ "join my support server : %s",
				Config.DEFAULT_PREFIX, Config.SUPPORT_SERVER_URL);

		return event.getMessage()
				.getChannel()
				.flatMap(channel -> {
					if(!channel.getLastMessageId().isPresent()) {
						BotUtils.sendMessage(text, channel);
						return Mono.empty();
					} else {
						return channel.getMessagesBefore(channel.getLastMessageId().get())
								// Return true if help text has not already been send
								.filter(historyMsg -> historyMsg.getContent().map(content -> !text.equalsIgnoreCase(content)).orElse(true))
								// Send the message even if an error t is re
								.doOnTerminate(() -> BotUtils.sendMessage(text, channel))
								.single();
					}
				})
				.then();
	}
}