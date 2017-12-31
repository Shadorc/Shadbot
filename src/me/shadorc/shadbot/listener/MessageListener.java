package me.shadorc.shadbot.listener;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.CommandManager;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.Database;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.shard.ShardManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.LogUtils;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;

public class MessageListener {

	@EventSubscriber
	public void onMessageEvent(MessageEvent event) {
		ShardManager.getThreadPool(event.getGuild()).execute(() -> {
			if(event instanceof MessageReceivedEvent) {
				this.onMessageReceivedEvent((MessageReceivedEvent) event);
			}
		});
	}

	private void onMessageReceivedEvent(MessageReceivedEvent event) {
		IMessage message = event.getMessage();
		try {
			if(message.getAuthor().isBot()) {
				return;
			}

			if(message.getChannel().isPrivate()) {
				this.privateMessageReceived(message);
				return;
			}

			if(!BotUtils.isChannelAllowed(event.getGuild(), event.getChannel())) {
				return;
			}

			// if(MessageManager.isWaitingForMessage(event.getChannel()) && MessageManager.notify(message)) {
			// return;
			// }

			String prefix = Database.getDBGuild(message.getGuild()).getPrefix();
			if(message.getContent().startsWith(prefix)) {
				CommandManager.execute(new Context(message));
			}
		} catch (Exception err) {
			//TODO: Bad, do we have to warn user ? In this case, don't include ID
			LogUtils.errorf(String.format("{Guild ID: %d} An unknown error occurred while receiving a message.", event.getGuild().getLongID()),
					err, message.getContent(), event.getChannel());
		}
	}

	private void privateMessageReceived(IMessage message) throws IllegalArgumentException, MissingArgumentException {
		if(message.getContent().startsWith(Config.DEFAULT_PREFIX + "help")) {
			CommandManager.getCommand("help").execute(new Context(message));
			return;
		}

		// If Shadbot didn't already send a message
		if(!message.getChannel().getMessageHistory().stream().anyMatch(msg -> msg.getAuthor().equals(message.getClient().getOurUser()))) {
			BotUtils.sendMessage("Hello !"
					+ "\nCommands only work in a server but you can see help using `" + Config.DEFAULT_PREFIX + "help`."
					+ "\nIf you have a question, a suggestion or if you just want to talk, don't hesitate to "
					+ "join my support server : " + Config.SUPPORT_SERVER, message.getChannel());
		}
	}
}