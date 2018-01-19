package me.shadorc.shadbot.listener;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.CommandManager;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.data.stats.Stats.VariousEnum;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.message.MessageManager;
import me.shadorc.shadbot.shard.ShardManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.TextUtils;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.MissingPermissionsException;

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
		StatsManager.increment(VariousEnum.MESSAGES_RECEIVED);

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

			if(!BotUtils.isChannelAllowed(message.getGuild(), message.getChannel())) {
				return;
			}

			if(MessageManager.intercept(message)) {
				return;
			}

			String prefix = Database.getDBGuild(message.getGuild()).getPrefix();
			if(message.getContent().startsWith(prefix)) {
				CommandManager.execute(new Context(message));
			}
		} catch (MissingPermissionsException err) {
			BotUtils.sendMessage(TextUtils.missingPerm(err.getMissingPermissions()), message.getChannel());
			LogUtils.infof("{Guild ID: %d} %s", message.getGuild().getLongID(), err.getMessage());
		} catch (Exception err) {
			LogUtils.errorf(message.getContent(), message.getChannel(), err,
					"Sorry, an unknown error occurred. My developer has been warned.", message.getGuild().getLongID());
		}
	}

	private void privateMessageReceived(IMessage message) throws MissingArgumentException, IllegalCmdArgumentException {
		StatsManager.increment(VariousEnum.PRIVATE_MESSAGES_RECEIVED);

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