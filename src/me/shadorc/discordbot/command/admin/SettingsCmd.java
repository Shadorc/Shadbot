package me.shadorc.discordbot.command.admin;

import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.Setting;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.EmbedBuilder;

public class SettingsCmd extends Command {

	private static final int PREFIX_MAX_LENGTH = 5;

	public SettingsCmd() {
		super(true, "settings", "setting", "options", "option");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(context.getArg() == null) {
			throw new MissingArgumentException();
		}

		String[] args = context.getArg().split(" ", 2);
		String name = args[0];

		if(name.equals("prefix")) {
			if(args.length != 2) {
				throw new MissingArgumentException();
			}

			String prefix = args[1].trim();
			if(prefix.contains(" ")) {
				BotUtils.sendMessage(Emoji.WARNING + " Prefix cannot contain space.", context.getChannel());
				return;
			}

			if(prefix.length() > PREFIX_MAX_LENGTH) {
				BotUtils.sendMessage(Emoji.WARNING + " Prefix cannot contain more than " + PREFIX_MAX_LENGTH + " characters.", context.getChannel());
				return;
			}

			Storage.storeSetting(context.getGuild(), Setting.PREFIX, prefix);
			BotUtils.sendMessage(Emoji.CHECK_MARK + " '" + prefix + "' is now the prefix for this server.", context.getChannel());

		} else if(name.equals("allowed_channels")) {
			List<IChannel> channels = context.getMessage().getChannelMentions();
			if(channels.size() == 0) {
				throw new MissingArgumentException();
			}

			JSONArray channelsArray = (JSONArray) Storage.getSetting(context.getGuild(), Setting.ALLOWED_CHANNELS);
			if(channelsArray == null) {
				channelsArray = new JSONArray();
			}

			for(IChannel channel : channels) {
				if(!BotUtils.isChannelAllowed(context.getGuild(), channel)) {
					channelsArray.put(channel.getStringID());
				}
			}
			Storage.storeSetting(context.getGuild(), Setting.ALLOWED_CHANNELS, channelsArray);
			BotUtils.sendMessage(Emoji.CHECK_MARK + " Channel(s) " + channels.stream().map(channel -> channel.mention()).collect(Collectors.joining(", ")).trim() + " have been added to the list of allowed channels.", context.getChannel());

		} else {
			throw new MissingArgumentException();
		}

	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Change Shadbot's server settings.**")
				.appendField("Name: prefix", "**arg:**prefix\n**Description:** Change Shadbot's prefix for this server.\n**Example:** " + context.getPrefix() + "settings prefix !", false)
				.appendField("Name: allowed_channels", "**arg:**#channel(s)\n**Description:** Allow Shadbot to only post messages in the mentioned channels. By default, all the channels are allowed.\n**Example:** " + context.getPrefix() + "settings allowed_channels #general", false)
				.appendField("Usage", context.getPrefix() + "settings <name> <arg>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
