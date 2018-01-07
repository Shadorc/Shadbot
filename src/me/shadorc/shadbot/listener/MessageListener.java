package me.shadorc.shadbot.listener;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.CommandManager;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.message.MessageManager;
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

			ShardManager.getShadbotShard(message.getShard()).messageReceived();

			if(!BotUtils.isChannelAllowed(event.getGuild(), event.getChannel())) {
				return;
			}

			if(MessageManager.intercept(message)) {
				return;
			}

			String prefix = Database.getDBGuild(message.getGuild()).getPrefix();
			if(message.getContent().startsWith(prefix)) {
				CommandManager.execute(new Context(message));
			}
		} catch (Exception err) {
			// TODO improve message
			LogUtils.errorf(message.getContent(), message.getChannel(), err,
					"Sorry, an unknown error occurred. My developer has been warned.", event.getGuild().getLongID());
		}
	}

	private void privateMessageReceived(IMessage message) throws MissingArgumentException, IllegalCmdArgumentException {
		if(message.getContent().startsWith(Config.DEFAULT_PREFIX + "help")) {
			CommandManager.getCommand("help").execute(new Context(message));
			return;
		}

		// If Shadbot didn't already send a message
		if(!message.getChannel().getMessageHistory().stream().anyMatch(msg -> msg.getAuthor().equals(message.getClient().getOurUser()))) {
			BotUtils.sendMessage(String.format("Hello !"
					+ "%nCommands only work in a server but you can see help using `%shelp`."
					+ "%nIf you have a question, a suggestion or if you just want to talk, don't hesitate to "
					+ "join my support server : %s", Config.DEFAULT_PREFIX, Config.SUPPORT_SERVER), message.getChannel());
		}
	}
}