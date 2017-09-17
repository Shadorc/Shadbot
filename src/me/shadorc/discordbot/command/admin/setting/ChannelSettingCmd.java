package me.shadorc.discordbot.command.admin.setting;

import java.util.List;

import org.json.JSONArray;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.Storage;
import me.shadorc.discordbot.data.Storage.Setting;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
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

		JSONArray allowedChannels = (JSONArray) Storage.getSetting(context.getGuild(), Setting.ALLOWED_CHANNELS);

		switch (arg.split(" ")[0]) {
			case "add":
				if(allowedChannels.length() == 0
						&& mentionedChannels.stream().filter(channel -> channel.getLongID() == context.getChannel().getLongID()).count() == 0) {
					BotUtils.sendMessage(Emoji.WARNING + " You did not mentioned this channel. "
							+ "I will not reply until it's added to the list of allowed channels.", context.getChannel());
				}

				for(IChannel channel : mentionedChannels) {
					allowedChannels.put(channel.getLongID());
				}

				BotUtils.sendMessage(Emoji.CHECK_MARK + " Channel "
						+ StringUtils.formatList(mentionedChannels, channel -> channel.mention(), ", ")
						+ " has been added to allowed channels.", context.getChannel());
				break;

			case "remove":
				List<Long> newAllowedChannels = Utils.convertToLongList(allowedChannels);
				for(IChannel channel : mentionedChannels) {
					newAllowedChannels.remove(channel.getLongID());
				}
				allowedChannels = new JSONArray(newAllowedChannels);

				BotUtils.sendMessage(Emoji.CHECK_MARK + " Channel "
						+ StringUtils.formatList(mentionedChannels, channel -> channel.mention(), ", ")
						+ " has been removed from allowed channels.", context.getChannel());
				break;

			default:
				BotUtils.sendMessage(Emoji.EXCLAMATION + " Invalid action. Use `" + context.getPrefix() + "settings "
						+ Setting.ALLOWED_CHANNELS.toString() + " help` to see help.", context.getChannel());
				return;
		}
		Storage.saveSetting(context.getGuild(), Setting.ALLOWED_CHANNELS, allowedChannels);
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed()
				.withAuthorName("Help for setting: " + Setting.ALLOWED_CHANNELS.toString())
				.appendDescription("**" + this.getDescription() + "**")
				.appendField("Argument", "**action** - add/remove"
						+ "\n**channel(s)** - the channel(s) to add/remove", false)
				.appendField("Usage", "`" + context.getPrefix() + "settings " + Setting.ALLOWED_CHANNELS.toString() + " <action> <#channel(s)>`", false)
				.appendField("Example", "`" + context.getPrefix() + "settings " + Setting.ALLOWED_CHANNELS.toString() + " add #general`", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

	@Override
	public String getDescription() {
		return "Allow Shadbot to only post messages in the mentioned channels."
				+ "\nBy default all channels are allowed.";
	}

}
