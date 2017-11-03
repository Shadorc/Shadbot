package me.shadorc.discordbot.command.admin.setting;

import java.util.List;

import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.data.Setting;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.EmbedBuilder;

public class AutoMessageSettingCmd implements SettingCmd {

	@Override
	public void execute(Context context, String arg) throws MissingArgumentException {
		if(arg == null) {
			throw new MissingArgumentException();
		}

		String[] splitArgs = StringUtils.getSplittedArg(arg, 2);
		if(splitArgs.length != 2) {
			throw new MissingArgumentException();
		}

		String arg1 = splitArgs[0];
		String arg2 = splitArgs[1];
		switch (arg1) {
			case "channel":
				List<IChannel> channelsMentioned = context.getMessage().getChannelMentions();
				if(channelsMentioned.isEmpty()) {
					throw new MissingArgumentException();
				}

				DatabaseManager.setSetting(context.getGuild(), Setting.MESSAGE_CHANNEL_ID, channelsMentioned.get(0).getLongID());
				BotUtils.sendMessage(Emoji.CHECK_MARK + " " + channelsMentioned.get(0).mention()
						+ " is now the channel for join/leave messages.", context.getChannel());
				break;

			case "join":
				if("disable".equals(arg2)) {
					DatabaseManager.removeSetting(context.getGuild(), Setting.JOIN_MESSAGE);
					BotUtils.sendMessage(Emoji.CHECK_MARK + " Join message disable.", context.getChannel());
				} else {
					DatabaseManager.setSetting(context.getGuild(), Setting.JOIN_MESSAGE, arg2);
					BotUtils.sendMessage(Emoji.CHECK_MARK + " The welcome message for this server is now: \"" + arg2 + "\".", context.getChannel());
					if(DatabaseManager.getSetting(context.getGuild(), Setting.MESSAGE_CHANNEL_ID) == null) {
						BotUtils.sendMessage(Emoji.INFO + " Use `" + context.getPrefix() + "settings " + Setting.AUTO_MESSAGE
								+ " channel <#channel>` to define in which channel auto messages are send.", context.getChannel());
					}
				}
				break;

			case "leave":
				if("disable".equals(arg2)) {
					DatabaseManager.removeSetting(context.getGuild(), Setting.LEAVE_MESSAGE);
					BotUtils.sendMessage(Emoji.CHECK_MARK + " Leave message disable.", context.getChannel());
				} else {
					DatabaseManager.setSetting(context.getGuild(), Setting.LEAVE_MESSAGE, arg2);
					BotUtils.sendMessage(Emoji.CHECK_MARK + " The goodbye message for this server is now: \"" + arg2 + "\".", context.getChannel());
					if(DatabaseManager.getSetting(context.getGuild(), Setting.MESSAGE_CHANNEL_ID) == null) {
						BotUtils.sendMessage(Emoji.INFO + " Use `" + context.getPrefix() + "settings " + Setting.AUTO_MESSAGE
								+ " channel <#channel>` to define in which channel auto messages are send.", context.getChannel());
					}
				}
				break;

			default:
				BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid command, use `" + context.getPrefix() + "settings "
						+ Setting.AUTO_MESSAGE.toString() + " help` to see help.", context.getChannel());
				return;
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed()
				.withAuthorName("Help for setting: " + Setting.AUTO_MESSAGE.toString())
				.appendDescription("__**Channel**__ (Define in which channel send auto messages)"
						+ "\n**Usage**"
						+ "\n`" + context.getPrefix() + "settings " + Setting.AUTO_MESSAGE + " channel <#channel>`"
						+ "\n**Argument**"
						+ "\n`channel(s)` - the channel in which to post auto messages"
						+ "\n**Example**"
						+ "\n`" + context.getPrefix() + "settings " + Setting.AUTO_MESSAGE + " channel #general`"
						+ "\n\n------------------------------------------------------------------"
						+ "\n\n__**Message**__ (Define message to send on user join/leave)"
						+ "\n**Usage**"
						+ "\n`" + context.getPrefix() + "settings " + Setting.AUTO_MESSAGE.toString() + " <event> <message>`"
						+ "\n**Argument**"
						+ "\n`event` - join/leave"
						+ "\n`message` - the message to display when an user join/leave"
						+ "\n**Example**"
						+ "\n`" + context.getPrefix() + "settings " + Setting.AUTO_MESSAGE.toString() + " join Hello you !`"
						+ "\n\n------------------------------------------------------------------"
						+ "\n\n__**Disable**__ (Disable auto message)"
						+ "\n**Usage**"
						+ "\n`" + context.getPrefix() + "settings " + Setting.AUTO_MESSAGE.toString() + " <event> disable`"
						+ "\n**Argument**"
						+ "\n`event` - join/leave"
						+ "\n**Example**"
						+ "\n`" + context.getPrefix() + "settings " + Setting.AUTO_MESSAGE.toString() + " leave disable`");

		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

	@Override
	public String getDescription() {
		return "Define auto messages on user join/leave.";
	}
}
