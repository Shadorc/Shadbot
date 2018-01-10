package me.shadorc.shadbot.command.admin.setting;

import java.util.List;

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
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.EmbedBuilder;

@Setting(description = "Define auto messages on user join/leave.", setting = SettingEnum.AUTO_MESSAGE)
public class AutoMessageSetting extends AbstractSetting {

	private enum Action {
		SET, REMOVE;
	}

	private enum Type {
		CHANNEL, JOIN_MESSAGE, LEAVE_MESSAGE;
	}

	@Override
	public void execute(Context context, String arg) throws MissingArgumentException, IllegalCmdArgumentException {
		List<String> splitArgs = StringUtils.split(arg, 3);
		if(splitArgs.size() < 2) {
			throw new MissingArgumentException();
		}

		Action action = Utils.getValueOrNull(Action.class, splitArgs.get(0));
		if(action == null) {
			throw new IllegalCmdArgumentException(String.format("Invalid action. Use `%s%s help` to see help.",
					context.getPrefix(), this.getCmdName()));
		}

		Type type = Utils.getValueOrNull(Type.class, splitArgs.get(1));
		if(type == null) {
			throw new IllegalCmdArgumentException(String.format("Invalid type. Use `%s%s help` to see help.",
					context.getPrefix(), this.getCmdName()));
		}

		switch (type) {
			case CHANNEL:
				this.channel(context, action);
				break;
			case JOIN_MESSAGE:
				this.updateJoinMessage(context, action, splitArgs);
				break;
			case LEAVE_MESSAGE:
				this.updateLeaveMessage(context, action, splitArgs);
				break;
		}
	}

	private void channel(Context context, Action action) throws MissingArgumentException {
		List<IChannel> channelsMentioned = context.getMessage().getChannelMentions();
		if(channelsMentioned.size() != 1) {
			throw new MissingArgumentException();
		}

		DBGuild dbGuild = Database.getDBGuild(context.getGuild());

		IChannel channel = channelsMentioned.get(0);
		if(Action.SET.equals(action)) {
			dbGuild.setSetting(SettingEnum.MESSAGE_CHANNEL_ID, channel.getLongID());
			BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " %s is now the default channel for join/leave messages.",
					channel.mention()), context.getChannel());
		} else if(Action.REMOVE.equals(action)) {
			dbGuild.removeSetting(SettingEnum.MESSAGE_CHANNEL_ID);
			BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Auto-messages disabled. I will no longer send automatic messages "
					+ "until a new channel is defined.", channel.mention()), context.getChannel());
		}
	}

	private void updateJoinMessage(Context context, Action action, List<String> args) {
		DBGuild dbGuild = Database.getDBGuild(context.getGuild());
		if(Action.SET.equals(action)) {
			String message = args.get(3);
			dbGuild.setSetting(SettingEnum.JOIN_MESSAGE, message);
			BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Join message set to \"%s\"", message), context.getChannel());

		} else if(Action.REMOVE.equals(action)) {
			dbGuild.removeSetting(SettingEnum.JOIN_MESSAGE);
			BotUtils.sendMessage(Emoji.CHECK_MARK + " Join message disabled.", context.getChannel());
		}
	}

	private void updateLeaveMessage(Context context, Action action, List<String> args) {
		DBGuild dbGuild = Database.getDBGuild(context.getGuild());
		if(Action.SET.equals(action)) {
			String message = args.get(3);
			dbGuild.setSetting(SettingEnum.LEAVE_MESSAGE, message);
			BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Leave message set to \"%s\"", message), context.getChannel());

		} else if(Action.REMOVE.equals(action)) {
			dbGuild.removeSetting(SettingEnum.LEAVE_MESSAGE);
			BotUtils.sendMessage(Emoji.CHECK_MARK + " Leave message disabled.", context.getChannel());
		}
	}

	@Override
	public EmbedBuilder getHelp(String prefix) {
		return EmbedUtils.getDefaultEmbed()
				.appendField("Usage", String.format("`%s%s <action> <type> [<message>]`", prefix, this.getCmdName()), false)
				.appendField("Argument", String.format("**action** - %s%n**type** - %s",
						FormatUtils.format(Action.values(), action -> action.toString().toLowerCase(), "/"),
						FormatUtils.format(Type.values(), type -> type.toString().toLowerCase(), "/")), false)
				.appendField("Example", String.format("`%s%s set join_message Hello you (:`", prefix, this.getCmdName()), false);
	}
}
