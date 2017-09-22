package me.shadorc.discordbot.events;

import me.shadorc.discordbot.command.CommandManager;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.data.Storage;
import me.shadorc.discordbot.data.Storage.Setting;
import me.shadorc.discordbot.message.MessageManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.command.Emoji;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;

@SuppressWarnings("ucd")
public class MessageListener {

	@EventSubscriber
	public void onMessageReceivedEvent(MessageReceivedEvent event) {
		if(Config.VERSION.isBeta() && event.getChannel().getLongID() != Config.DEBUG_CHANNEL_ID
				|| !Config.VERSION.isBeta() && event.getChannel().getLongID() == Config.DEBUG_CHANNEL_ID) {
			return;
		}

		if(event.getAuthor().isBot()) {
			return;
		}

		if(event.getChannel().isPrivate()) {
			BotUtils.sendMessage(Emoji.INFO + " Sorry, I don't respond (yet ?) to private messages.", event.getChannel());
			LogUtils.info("Shadbot has received a private message. (Message: " + event.getMessage().getContent() + ")");
			return;
		}

		if(!BotUtils.isChannelAllowed(event.getGuild(), event.getChannel())) {
			return;
		}

		IMessage message = event.getMessage();
		if(MessageManager.isWaitingForMessage(event.getChannel()) && MessageManager.notify(message)) {
			return;
		}

		String prefix = Storage.getSetting(event.getGuild(), Setting.PREFIX).toString();
		if(message.getContent().startsWith(prefix)) {
			CommandManager.getInstance().manage(event);
		}
	}
}