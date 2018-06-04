package me.shadorc.shadbot.listener;

import java.util.Collections;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Channel.Type;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.CommandManager;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.data.stats.VariousStatsManager;
import me.shadorc.shadbot.data.stats.VariousStatsManager.VariousEnum;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.message.MessageManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Mono;

public class MessageListener {

	public static void onMessageCreate(MessageCreateEvent event) {
		VariousStatsManager.log(VariousEnum.MESSAGES_RECEIVED);

		Mono.just(event.getMessage())
				// Ignore webhook
				.filter(message -> message.getContent().isPresent()
						&& message.getAuthorId().isPresent())
				.flatMap(message -> message.getAuthor())
				// Do not answer to bot
				.filter(author -> !author.isBot())
				.flatMap(author -> event.getMessage().getChannel())
				.subscribe(channel -> {
					// The channel is a private channel
					if(channel.getType().equals(Type.DM)) {
						MessageListener.onPrivateMessage(channel, event.getMessage());
						return;
					}

					final Snowflake guildId = event.getGuildId().get();

					// The bot does not have the permission to access this channel
					if(!BotUtils.isChannelAllowed(guildId, channel.getId())) {
						return;
					}

					event.getMember().get()
							.getRoles()
							.buffer()
							.defaultIfEmpty(Collections.emptyList())
							// The author role is allowed to access to the bot
							.filter(roleList -> BotUtils.hasAllowedRole(guildId, roleList))
							// No listener have the priority on this listener and stopped the process
							.filter(roleList -> !MessageManager.intercept(event.getMessage()))
							.map(roleList -> Database.getDBGuild(guildId).getPrefix())
							// The message content start with the correct prefix
							.filter(prefix -> event.getMessage().getContent().get().startsWith(prefix))
							.subscribe(prefix -> CommandManager.execute(new Context(guildId, event.getMessage(), prefix)));
				});
	}

	private static void onPrivateMessage(MessageChannel channel, Message message) {
		VariousStatsManager.log(VariousEnum.PRIVATE_MESSAGES_RECEIVED);

		String msgContent = message.getContent().get();
		if(msgContent.startsWith(Config.DEFAULT_PREFIX + "help")) {
			try {
				CommandManager.getCommand("help").execute(new Context(null, message, Config.DEFAULT_PREFIX));
			} catch (MissingArgumentException | IllegalCmdArgumentException err) {
				LogUtils.error(msgContent, err,
						String.format("{Channel ID: %s} An unknown error occurred while showing help in a private channel.", channel.getId()));
			}
			return;
		}

		String text = String.format("Hello !"
				+ "%nCommands only work in a server but you can see help using `%shelp`."
				+ "%nIf you have a question, a suggestion or if you just want to talk, don't hesitate to "
				+ "join my support server : %s",
				Config.DEFAULT_PREFIX, Config.SUPPORT_SERVER_URL);

		if(!channel.getLastMessageId().isPresent()) {
			BotUtils.sendMessage(text, channel);
		} else {
			channel.getMessagesBefore(channel.getLastMessageId().get())
					// Return true if help text has not already been send
					.filter(historyMsg -> historyMsg.getContent().map(content -> !text.equalsIgnoreCase(content)).orElse(true))
					// Send the message even if an error occured
					.doOnTerminate(() -> BotUtils.sendMessage(text, channel))
					.subscribe();
		}

	}
}