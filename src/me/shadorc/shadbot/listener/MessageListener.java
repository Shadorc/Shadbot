package me.shadorc.shadbot.listener;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Channel.Type;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.CommandManager;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.data.stats.VariousStatsManager;
import me.shadorc.shadbot.data.stats.VariousStatsManager.VariousEnum;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.message.MessageManager;
import me.shadorc.shadbot.shard.ShardManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;

public class MessageListener {

	public static void onMessageCreate(MessageCreateEvent event) {
		VariousStatsManager.log(VariousEnum.MESSAGES_RECEIVED);

		Message message = event.getMessage();
		String content = message.getContent().orElse("");

		if(message.getAuthor().block().isBot()) {
			return;
		}

		MessageChannel channel = message.getChannel().block();
		if(channel.getType().equals(Type.DM)) {
			MessageListener.onPrivateMessage(message);
			return;
		}

		ShardManager.getShard(event.getClient()).messageReceived();

		Member member = message.getAuthorAsMember().block();
		Guild guild = message.getGuild().block();
		if(!BotUtils.hasAllowedRole(guild, member.getRoles().collectList().block())) {
			return;
		}

		if(!BotUtils.isChannelAllowed(guild, channel)) {
			return;
		}

		if(MessageManager.intercept(message)) {
			return;
		}

		String prefix = Database.getDBGuild(guild.getId()).getPrefix();
		if(content.startsWith(prefix)) {
			CommandManager.execute(new Context(prefix, message));
		}
	}

	private static void onPrivateMessage(Message message) {
		VariousStatsManager.log(VariousEnum.PRIVATE_MESSAGES_RECEIVED);

		String msgContent = message.getContent().orElse("");
		MessageChannel channel = message.getChannel().block();
		if(msgContent.startsWith(Config.DEFAULT_PREFIX + "help")) {
			try {
				CommandManager.getCommand("help").execute(new Context(Config.DEFAULT_PREFIX, message));
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