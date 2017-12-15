package me.shadorc.discordbot.events;

import java.util.Arrays;
import java.util.stream.Collectors;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.CommandManager;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.data.Setting;
import me.shadorc.discordbot.exceptions.MissingArgumentException;
import me.shadorc.discordbot.message.MessageManager;
import me.shadorc.discordbot.shards.ShardManager;
import me.shadorc.discordbot.stats.StatsEnum;
import me.shadorc.discordbot.stats.StatsManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.EmbedBuilder;

@SuppressWarnings("ucd")
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
			EmbedBuilder builder = Utils.getDefaultEmbed()
					.setLenient(true)
					.withAuthorName("Shadbot Help")
					.withFooterText("Any issues, questions or suggestions ? Join https://discord.gg/CKnV4ff");

			Arrays.stream(CommandCategory.values())
					.filter(cmdCat -> !cmdCat.equals(CommandCategory.HIDDEN))
					.forEach(category -> builder.appendField(category.toString() + " Commands:",
							CommandManager.getCommands().values().stream()
									.filter(cmd -> cmd.getCategory().equals(category)
											&& Role.ADMIN.getHierarchy() >= cmd.getRole().getHierarchy())
									.distinct()
									.map(cmd -> "`" + Config.DEFAULT_PREFIX + cmd.getFirstName() + "`")
									.collect(Collectors.joining(" ")), false));

			BotUtils.sendMessage(builder.build(), event.getChannel());
			return;
		}

		// If Shadbot didn't already send a message
		if(!event.getChannel().getMessageHistory().stream().anyMatch(msg -> msg.getAuthor().equals(Shadbot.getClient().getOurUser()))) {
			BotUtils.sendMessage("Hello !"
					+ "\nCommands only work in a server but you can see help using `/help`."
					+ "\nIf you have a question, a suggestion or anything, don't hesitate to "
					+ "join my support server : https://discord.gg/CKnV4ff", event.getChannel());
		}

		StatsManager.increment(StatsEnum.PRIVATE_MESSAGES_RECEIVED);
	}
}