package me.shadorc.discordbot.command.admin;

import java.util.List;

import org.json.JSONArray;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.Setting;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.EmbedBuilder;

public class SettingsCmd extends AbstractCommand {

	private static final int PREFIX_MAX_LENGTH = 5;

	public SettingsCmd() {
		super(Role.ADMIN, "settings", "setting", "options", "option");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		String[] args = context.getArg().split(" ", 2);
		String name = args[0];

		if(Setting.PREFIX.toString().equals(name)) {
			if(args.length != 2) {
				throw new MissingArgumentException();
			}

			String prefix = args[1].trim();
			if(prefix.contains(" ")) {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " Prefix cannot contain space.", context.getChannel());
				return;
			}

			if(prefix.length() > PREFIX_MAX_LENGTH) {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " Prefix cannot contain more than " + PREFIX_MAX_LENGTH + " characters.", context.getChannel());
				return;
			}

			Storage.saveSetting(context.getGuild(), Setting.PREFIX, prefix);
			BotUtils.sendMessage(Emoji.CHECK_MARK + " '" + prefix + "' is now the prefix for this server.", context.getChannel());

		} else if(Setting.ALLOWED_CHANNELS.toString().equals(name)) {
			List<IChannel> mentionedChannels = context.getMessage().getChannelMentions();
			if(mentionedChannels.isEmpty()) {
				throw new MissingArgumentException();
			}

			JSONArray allowedChannelsArray = (JSONArray) Storage.getSetting(context.getGuild(), Setting.ALLOWED_CHANNELS);
			for(IChannel channel : mentionedChannels) {
				allowedChannelsArray.put(channel.getStringID());
			}

			Storage.saveSetting(context.getGuild(), Setting.ALLOWED_CHANNELS, allowedChannelsArray);
			BotUtils.sendMessage(Emoji.CHECK_MARK + " Channel(s) "
					+ StringUtils.formatList(mentionedChannels, channel -> channel.mention(), ", ")
					+ " have been added to the list of allowed channels.", context.getChannel());

		} else if(Setting.DEFAULT_VOLUME.toString().equals(name)) {
			if(args.length != 2) {
				throw new MissingArgumentException();
			}

			String volStr = args[1].trim();
			if(!StringUtils.isPositiveInt(volStr)) {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " Volume number isn't valid.", context.getChannel());
				return;
			}

			int vol = Integer.parseInt(volStr);
			if(vol < 1 || vol > 50) {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " Default volume must be between 1 and 50. ", context.getChannel());
				return;
			}

			Storage.saveSetting(context.getGuild(), Setting.DEFAULT_VOLUME, vol);
			BotUtils.sendMessage(Emoji.CHECK_MARK + " " + vol + "% is now the default volume for this server.", context.getChannel());

		} else {
			throw new MissingArgumentException();
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.withThumbnail("http://www.emoji.co.uk/files/emoji-one/objects-emoji-one/1898-gear.png")
				.appendDescription("**Change Shadbot's server settings.**")
				.appendField("Name: " + Setting.PREFIX.toString(),
						"**Description:** Change Shadbot's prefix."
								+ "\n**arg:** prefix (Max length: 5, must not contain spaces)"
								+ "\n**Example:** " + context.getPrefix() + "settings " + Setting.PREFIX.toString() + " !", false)
				.appendField("Name: " + Setting.ALLOWED_CHANNELS.toString(),
						"**Description:** Allow Shadbot to only post messages in the mentioned channels. By default all channels are allowed."
								+ "\n**arg:** #channel(s)"
								+ "\n**Example:** " + context.getPrefix() + "settings " + Setting.ALLOWED_CHANNELS.toString() + " #general", false)
				.appendField("Name: " + Setting.DEFAULT_VOLUME.toString(),
						"**Description:** Change music default volume."
								+ "\n**arg:** volume (Min: 1/Max: 50/Default: " + Config.DEFAULT_VOLUME + ")"
								+ "\n**Example:** " + context.getPrefix() + "settings " + Setting.DEFAULT_VOLUME.toString() + " 42", false)
				.appendField("Usage", context.getPrefix() + "settings <name> <arg>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
