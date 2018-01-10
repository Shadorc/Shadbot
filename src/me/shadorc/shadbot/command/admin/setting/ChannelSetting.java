package me.shadorc.shadbot.command.admin.setting;

import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;

import me.shadorc.shadbot.command.admin.setting.core.AbstractSetting;
import me.shadorc.shadbot.command.admin.setting.core.Setting;
import me.shadorc.shadbot.command.admin.setting.core.SettingEnum;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.EmbedBuilder;

@Setting(description = "Allow Shadbot to post messages only in the mentioned channels.", setting = SettingEnum.ALLOWED_CHANNELS)
public class ChannelSetting extends AbstractSetting {

	private enum Action {
		ADD, REMOVE;
	}

	@Override
	public void execute(Context context, String arg) throws MissingArgumentException, IllegalCmdArgumentException {
		List<String> splitArgs = StringUtils.split(arg);
		if(splitArgs.size() < 2) {
			throw new MissingArgumentException();
		}

		List<IChannel> mentionedChannels = context.getMessage().getChannelMentions();
		if(mentionedChannels.isEmpty()) {
			throw new MissingArgumentException();
		}

		Action action = Utils.getValueOrNull(Action.class, splitArgs.get(0));
		if(action == null) {
			throw new IllegalCmdArgumentException(String.format("Invalid action. Use `%s%s help` to see help.",
					context.getPrefix(), this.getCmdName()));
		}

		List<Long> allowedChannelsList = Database.getDBGuild(context.getGuild()).getAllowedChannels();
		if(Action.ADD.equals(action)) {
			if(allowedChannelsList.isEmpty()
					&& mentionedChannels.stream().noneMatch(channel -> channel.getLongID() == context.getChannel().getLongID())) {
				BotUtils.sendMessage(Emoji.WARNING + " You did not mentioned this channel. "
						+ "I will not reply until it's added to the list of allowed channels.", context.getChannel());
			}

			allowedChannelsList.addAll(mentionedChannels.stream()
					.map(IChannel::getLongID)
					.filter(channelID -> !allowedChannelsList.contains(channelID))
					.collect(Collectors.toList()));

			BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Channel %s has been added to allowed channels.",
					FormatUtils.format(mentionedChannels, IChannel::mention, ", ")), context.getChannel());

		} else {
			allowedChannelsList.removeAll(mentionedChannels.stream().map(IChannel::getLongID).collect(Collectors.toList()));
			// TODO: improve has/have
			BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Channel %s has been removed from allowed channels.",
					FormatUtils.format(mentionedChannels, IChannel::mention, ", ")), context.getChannel());
		}

		Database.getDBGuild(context.getGuild()).setSetting(this.getSetting(), new JSONArray(allowedChannelsList));
	}

	@Override
	public EmbedBuilder getHelp(String prefix) {
		return EmbedUtils.getDefaultEmbed()
				.appendField("Usage", String.format("`%s%s <action> <#channel(s)>`", prefix, this.getCmdName()), false)
				.appendField("Argument", String.format("**action** - %s%n**channel(s)** - the channel(s) to %s",
						FormatUtils.format(Action.values(), action -> action.toString().toLowerCase(), "/"),
						FormatUtils.format(Action.values(), action -> action.toString().toLowerCase(), "/")), false)
				.appendField("Example", String.format("`%s%s add #general`", prefix, this.getCmdName()), false);
	}

}
