package me.shadorc.shadbot.command.admin.setting;

import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;

import me.shadorc.shadbot.command.admin.setting.core.AbstractSetting;
import me.shadorc.shadbot.command.admin.setting.core.Setting;
import me.shadorc.shadbot.command.admin.setting.core.SettingEnum;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.db.DBGuild;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.Emoji;

@Setting(description = "Manage channels allowed to Shadbot.", setting = SettingEnum.ALLOWED_CHANNELS)
public class ChannelSetting extends AbstractSetting {

	private enum Action {
		ADD, REMOVE;
	}

	@Override
	public void execute(Context context, String arg) throws MissingArgumentException, IllegalCmdArgumentException {
		if(arg == null) {
			throw new MissingArgumentException();
		}

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
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid action. %s",
					splitArgs.get(0), FormatUtils.formatOptions(Action.class)));
		}

		DBGuild dbGuild = Database.getDBGuild(context.getGuild());
		List<Long> allowedChannelsList = dbGuild.getAllowedChannels();
		if(Action.ADD.equals(action)) {
			if(allowedChannelsList.isEmpty()
					&& mentionedChannels.stream().noneMatch(channel -> channel.getLongID() == context.getChannel().getLongID())) {
				BotUtils.sendMessage(Emoji.WARNING + " You did not mentioned this channel. "
						+ "I will not reply here until this channel is added to the list of allowed channels.", context.getChannel());
			}

			allowedChannelsList.addAll(mentionedChannels.stream()
					.map(IChannel::getLongID)
					.filter(channelID -> !allowedChannelsList.contains(channelID))
					.collect(Collectors.toList()));

			BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Channel %s added to allowed channels.",
					FormatUtils.format(mentionedChannels, IChannel::mention, ", ")), context.getChannel());

		} else {
			allowedChannelsList.removeAll(mentionedChannels.stream().map(IChannel::getLongID).collect(Collectors.toList()));
			BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Channel %s removed from allowed channels.",
					FormatUtils.format(mentionedChannels, IChannel::mention, ", ")), context.getChannel());
		}

		dbGuild.setSetting(this.getSetting(), new JSONArray(allowedChannelsList));
	}

	@Override
	public EmbedBuilder getHelp(String prefix) {
		return EmbedUtils.getDefaultEmbed()
				.addField("Usage", String.format("`%s%s <action> <#channel(s)>`", prefix, this.getCmdName()), false)
				.addField("Argument", String.format("**action** - %s%n**channel(s)** - the channel(s) to %s",
						FormatUtils.format(Action.values(), action -> action.toString().toLowerCase(), "/"),
						FormatUtils.format(Action.values(), action -> action.toString().toLowerCase(), "/")), false)
				.addField("Example", String.format("`%s%s add #general`", prefix, this.getCmdName()), false);
	}

}
