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
			.filter(msg -> msg.getContent().isPresent() && msg.getAuthorId().isPresent())
			.flatMap(Message::getAuthor)
			.filter(user -> !user.isBot())
			.flatMap(author -> event.getMessage().getChannel())
			.filter(chnl -> chnl.getType().equals(Type.DM))
			.switchIfEmpty(Mono.fromRunnable(() -> MessageListener.onPrivateMessage(event.getMessage())))
			.filter(chnl -> BotUtils.isChannelAllowed(guildId.get(), chnl.getId()))
			.flatMapMany(channel -> event.getMember().get().getRoles().buffer())
			.defaultIfEmpty(Collections.emptyList())
			.single()
			.filter(roles -> BotUtils.hasAllowedRole(guildId.get(), roles))
			.filter(roles -> !MessageManager.intercept(guildId.get(), event.getMessage()))
			.map(roles -> Database.getDBGuild(guildId.get()).getPrefix())
			.filter(prefix -> event.getMessage().getContent().get().startsWith(prefix))
			.doOnSuccess(prefix -> CommandManager.execute(new Context(guildId.get(), event.getMessage(), prefix)))
			.subscribe();
	}

	private static Mono<Void> onPrivateMessage(Message message) {
		VariousStatsManager.log(VariousEnum.PRIVATE_MESSAGES_RECEIVED);

		String msgContent = message.getContent().get();
		if(msgContent.startsWith(Config.DEFAULT_PREFIX + "help")) {
			CommandManager.getCommand("help").execute(new Context(null, message, Config.DEFAULT_PREFIX));
			return Mono.empty();
		}

		final String text = String.format("Hello !"
				+ "%nCommands only work in a server but you can see help using `%shelp`."
				+ "%nIf you have a question, a suggestion or if you just want to talk, don't hesitate to "
				+ "join my support server : %s",
				Config.DEFAULT_PREFIX, Config.SUPPORT_SERVER_URL);

		return message.getChannel()
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