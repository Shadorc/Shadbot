package me.shadorc.discordbot.command.admin.setting;

import java.util.List;

import org.json.JSONArray;

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

public class ChannelSettingCmd implements SettingCmd {

	@Override
	public void execute(Context context, String arg) throws MissingArgumentException {
		if(arg == null) {
			throw new MissingArgumentException();
		}

		List<IChannel> mentionedChannels = context.getMessage().getChannelMentions();
		if(mentionedChannels.isEmpty()) {
			throw new MissingArgumentException();
		}

		List<Long> allowedChannelsList = Utils.convertToList((JSONArray) DatabaseManager.getSetting(context.getGuild(), Setting.ALLOWED_CHANNELS), Long.class);

		switch (arg.split(" ")[0]) {
			case "add":
				if(allowedChannelsList.isEmpty()
						&& mentionedChannels.stream().filter(channel -> channel.getLongID() == context.getChannel().getLongID()).count() == 0) {
					BotUtils.sendMessage(Emoji.WARNING + " You did not mentioned this channel. "
							+ "I will not reply until it's added to the list of allowed channels.", context.getChannel());
				}

				for(IChannel channel : mentionedChannels) {
					if(!allowedChannelsList.contains(channel.getLongID())) {
						allowedChannelsList.add(channel.getLongID());
					}
				}

				BotUtils.sendMessage(Emoji.CHECK_MARK + " Channel "
						+ StringUtils.formatList(mentionedChannels, channel -> channel.mention(), ", ")
						+ " has been added to allowed channels.", context.getChannel());
				break;

			case "remove":
				for(IChannel channel : mentionedChannels) {
					allowedChannelsList.remove(channel.getLongID());
				}

				BotUtils.sendMessage(Emoji.CHECK_MARK + " Channel "
						+ StringUtils.formatList(mentionedChannels, channel -> channel.mention(), ", ")
						+ " has been removed from allowed channels.", context.getChannel());
				break;

			default:
				BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid action. Use `" + context.getPrefix() + "settings "
						+ Setting.ALLOWED_CHANNELS.toString() + " help` to see help.", context.getChannel());
				return;
		}
		DatabaseManager.setSetting(context.getGuild(), Setting.ALLOWED_CHANNELS, new JSONArray(allowedChannelsList));
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed()
				.withAuthorName("Help for setting: " + Setting.ALLOWED_CHANNELS.toString())
				.appendDescription("**" + this.getDescription() + "**")
				.appendField("Usage", "`" + context.getPrefix() + "settings " + Setting.ALLOWED_CHANNELS.toString() + " <action> <#channel(s)>`", false)
				.appendField("Argument", "**action** - add/remove"
						+ "\n**channel(s)** - the channel(s) to add/remove", false)
				.appendField("Example", "`" + context.getPrefix() + "settings " + Setting.ALLOWED_CHANNELS.toString() + " add #general`", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

	@Override
	public String getDescription() {
		return "Allow Shadbot to post messages only in the mentioned channels.";
	}

}
