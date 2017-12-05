package me.shadorc.discordbot.events;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.CommandManager;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.data.Setting;
import me.shadorc.discordbot.message.MessageManager;
import me.shadorc.discordbot.stats.StatsEnum;
import me.shadorc.discordbot.stats.StatsManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;

@SuppressWarnings("ucd")
public class MessageListener {

	@EventSubscriber
	public void onMessageReceivedEvent(MessageReceivedEvent event) {
		IMessage message = event.getMessage();
		try {
			StatsManager.increment(StatsEnum.MESSAGES_RECEIVED);

			if(event.getAuthor().isBot()) {
				return;
			}

			if(event.getChannel().isPrivate()) {
				this.privateMessageReceived(event);
				return;
			}

			if(!BotUtils.isChannelAllowed(event.getGuild(), event.getChannel())) {
				return;
			}

			if(MessageManager.isWaitingForMessage(event.getChannel()) && MessageManager.notify(message)) {
				return;
			}

			String prefix = (String) DatabaseManager.getSetting(event.getGuild(), Setting.PREFIX);
			if(message.getContent().startsWith(prefix)) {
				CommandManager.manage(event);
			}
		} catch (Exception err) {
			LogUtils.error("{Guild ID: " + event.getGuild().getLongID() + "} An unknown error occurred while receiving a message.", err, message.getContent());
		}
	}

	private void privateMessageReceived(MessageReceivedEvent event) throws MissingArgumentException {
		if(event.getMessage().getContent().startsWith(Config.DEFAULT_PREFIX + "help")) {
			CommandManager.getCommand("help").execute(new Context(event));
			return;
		}

		// If Shadbot didn't already send a message
		if(!event.getChannel().getMessageHistory().stream().anyMatch(msg -> msg.getAuthor().equals(Shadbot.getClient().getOurUser()))) {
			BotUtils.sendMessage(Emoji.SPEECH + " Sorry, I don't reply to private messages but you can still send me some, "
					+ "my developer will be able to read them (:", event.getChannel());
		}

		BotUtils.sendMessage("{User ID: " + event.getAuthor().getLongID() + "} "
				+ event.getMessage().getContent().replace("://", ":// "), Shadbot.getClient().getChannelByID(Config.PM_CHANNEL_ID));
	}
}