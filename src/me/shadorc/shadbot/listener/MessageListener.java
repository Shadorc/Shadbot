package me.shadorc.shadbot.listener;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Channel.Type;
import discord4j.core.object.entity.Message;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer;
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
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.util.MissingPermissionsException;

public class MessageListener {

	public static class MessageCreateListener implements Consumer<MessageCreateEvent> {
		@Override
		public void accept(MessageCreateEvent event) {
			VariousStatsManager.log(VariousEnum.MESSAGES_RECEIVED);

			Message message = event.getMessage();
			String content = message.getContent().orElse("");
			long guildID = message.getGuild().block().getId().asLong();
			try {
				if(message.getAuthor().block().isBot()) {
					return;
				}

				if(message.getChannel().block().getType().equals(Type.DM)) {
					this.privateMessageReceived(message);
					return;
				}

				ShardManager.getShadbotShard(message.getShard()).messageReceived();

				if(!BotUtils.hasAllowedRole(message.getGuild(), message.getGuild().getRolesForUser(message.getAuthor()))) {
					return;
				}

				if(!BotUtils.isChannelAllowed(message.getGuild(), message.getChannel())) {
					return;
				}

				if(MessageManager.intercept(message)) {
					return;
				}

				String prefix = Database.getDBGuild(message.getGuild()).getPrefix();
				if(content.startsWith(prefix)) {
					CommandManager.execute(new Context(prefix, message));
				}
			} catch (MissingPermissionsException err) {
				BotUtils.sendMessage(TextUtils.missingPerm(err.getMissingPermissions()), message.getChannel());
				LogUtils.infof("{Guild ID: %d} %s", guildID, err.getMessage());
			} catch (Exception err) {
				BotUtils.sendMessage(Emoji.RED_FLAG + " Sorry, an unknown error occurred. My developer has been warned.", message.getChannel());
				LogUtils.error(content, err,
						String.format("{Guild ID: %d} An unknown error occurred while receiving a message.", guildID));
			}
		}
	}

	private void privateMessageReceived(Message message) throws MissingArgumentException, IllegalCmdArgumentException {
		VariousStatsManager.log(VariousEnum.PRIVATE_MESSAGES_RECEIVED);

		if(message.getContent().orElse("").startsWith(Config.DEFAULT_PREFIX + "help")) {
			CommandManager.getCommand("help").execute(new Context(Config.DEFAULT_PREFIX, message));
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