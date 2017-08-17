package me.shadorc.discordbot.command.admin;

import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.Setting;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.StringUtils;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.EmbedBuilder;

public class SettingsCmd extends AbstractCommand {

	private static final int PREFIX_MAX_LENGTH = 5;

	public SettingsCmd() {
		super(true, "settings", "setting", "options", "option");
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
			if(allowedChannelsArray == null) {
				allowedChannelsArray = new JSONArray();
			}

			for(IChannel channel : mentionedChannels) {
				allowedChannelsArray.put(channel.getStringID());
			}

			Storage.saveSetting(context.getGuild(), Setting.ALLOWED_CHANNELS, allowedChannelsArray);
			BotUtils.sendMessage(Emoji.CHECK_MARK + " Channel(s) " + mentionedChannels.stream().map(channel -> channel.mention()).collect(Collectors.joining(", ")).trim() + " have been added to the list of allowed channels.", context.getChannel());

		} else if(Setting.DEFAULT_VOLUME.toString().equals(name)) {
			if(args.length != 2) {
				throw new MissingArgumentException();
			}

			String volStr = args[1].trim();
			if(!StringUtils.isInteger(volStr)) {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " Please, enter a valid number.", context.getChannel());
				return;
			}

			int vol = Integer.parseInt(volStr);
			if(vol < 1 || vol > 50) {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " Default volume must be between 1 and 50. ", context.getChannel());
				return;
			}

			Storage.getSetting(context.getGuild(), Setting.DEFAULT_VOLUME);
			BotUtils.sendMessage(Emoji.CHECK_MARK + " " + vol + "% is now the default volume for this server.", context.getChannel());

		} else {
			throw new MissingArgumentException();
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Change Shadbot's server settings.**")
				.appendField("Name: prefix",
						"**Description:** Change Shadbot's prefix for this server."
								+ "\n**arg:** prefix"
								+ "\n**Example:** " + context.getPrefix() + "settings prefix !", false)
				.appendField("Name: allowed_channels",
						"**Description:** Allow Shadbot to only post messages in the mentioned channels. By default, all the channels are allowed."
								+ "\n**arg:** #channel(s)"
								+ "\n**Example:** " + context.getPrefix() + "settings allowed_channels #general", false)
				.appendField("Usage", context.getPrefix() + "settings <name> <arg>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
