package me.shadorc.shadbot.listener;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Channel.Type;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.TextChannel;
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

public class MessageListener {

	public static void onMessageCreate(MessageCreateEvent event) {

		VariousStatsManager.log(VariousEnum.MESSAGES_RECEIVED);

		Message message = event.getMessage();

		// The message is a webhook
		if(!message.getContent().isPresent() || !message.getAuthorId().isPresent()) {
			return;
		}

		message.getAuthor().subscribe(author -> {

			// The author is a bot
			if(author.isBot()) {
				return;
			}

			message.getChannel().subscribe(channel -> {

				// The channel is a private channel
				if(channel.getType().equals(Type.DM)) {
					MessageListener.onPrivateMessage(channel, message);
					return;
				}

				// The channel is not private, it can be casted to TextChannel to get guild ID
				Snowflake guildId = TextChannel.class.cast(channel).getGuildId();

				// The bot does not have the permission to access this channel
				if(!BotUtils.isChannelAllowed(guildId, channel.getId())) {
					return;
				}

				message.getAuthorAsMember().flatMapMany(Member::getRoles).buffer().subscribe(rolesList -> {

					// The author role is not allowed to access to the bot
					if(!BotUtils.hasAllowedRole(guildId, rolesList)) {
						return;
					}

					// A listener has the priority on this listener and stop the processing
					if(MessageManager.intercept(message)) {
						return;
					}

					// The message content is a command, execute it
					String prefix = Database.getDBGuild(guildId).getPrefix();
					if(message.getContent().get().startsWith(prefix)) {
						CommandManager.execute(new Context(guildId, message, prefix));
					}
				});
			});
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
						String.format("{Channel ID: %d} An unknown error occurred while showing help in a private channel.",
								channel.getId().asLong()));
			}
			return;
		}

		String text = String.format("Hello !"
				+ "%nCommands only work in a server but you can see help using `%shelp`."
				+ "%nIf you have a question, a suggestion or if you just want to talk, don't hesitate to "
				+ "join my support server : %s",
				Config.DEFAULT_PREFIX, Config.SUPPORT_SERVER_URL);

		channel.getLastMessageId().ifPresentOrElse(lastMsgId -> {
			channel.getMessagesBefore(lastMsgId)
					.map(Message::getContent)
					.map(content -> content.orElse(""))
					.any(text::equalsIgnoreCase)
					.subscribe(alreadySent -> {
						if(!alreadySent) {
							BotUtils.sendMessage(text, channel);
						}

					});
		}, () -> BotUtils.sendMessage(text, channel));

	}
}