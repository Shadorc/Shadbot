package me.shadorc.shadbot.command.admin.setting;

import java.util.List;

import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.setting.AbstractSetting;
import me.shadorc.shadbot.core.setting.Setting;
import me.shadorc.shadbot.core.setting.SettingEnum;
import me.shadorc.shadbot.data.database.DBGuild;
import me.shadorc.shadbot.data.database.DatabaseManager;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Setting(description = "Manage auto messages on user join/leave.", setting = SettingEnum.AUTO_MESSAGE)
public class AutoMessageSetting extends AbstractSetting {

	private enum Action {
		ENABLE, DISABLE;
	}

	private enum Type {
		CHANNEL, JOIN_MESSAGE, LEAVE_MESSAGE;
	}

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(3, 4);

		final Action action = Utils.getEnum(Action.class, args.get(1));
		if(action == null) {
			throw new CommandException(String.format("`%s` is not a valid action. %s", args.get(1), FormatUtils.options(Action.class)));
		}

		final Type type = Utils.getEnum(Type.class, args.get(2));
		if(type == null) {
			throw new CommandException(String.format("`%s` is not a valid type. %s", args.get(2), FormatUtils.options(Type.class)));
		}

		switch (type) {
			case CHANNEL:
				return this.channel(context, action).then();
			case JOIN_MESSAGE:
				return this.updateMessage(context, SettingEnum.JOIN_MESSAGE, action, args).then();
			case LEAVE_MESSAGE:
				return this.updateMessage(context, SettingEnum.LEAVE_MESSAGE, action, args).then();
			default:
				return Mono.empty();
		}
	}

	private Mono<Message> channel(Context context, Action action) {
		return DiscordUtils.getChannels(context.getGuild(), context.getContent())
				.flatMap(channelsMentionned -> {
					if(channelsMentionned.size() != 1) {
						throw new MissingArgumentException();
					}

					final DBGuild dbGuild = DatabaseManager.getDBGuild(context.getGuildId());
					final Snowflake channelId = channelsMentionned.get(0);
					if(Action.ENABLE.equals(action)) {
						dbGuild.setSetting(SettingEnum.MESSAGE_CHANNEL_ID, channelId);
						return BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " %s is now the default channel for join/leave messages.",
								DiscordUtils.mentionChannel(channelId)), context.getChannel());
					} else {
						dbGuild.removeSetting(SettingEnum.MESSAGE_CHANNEL_ID);
						return BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Auto-messages disabled. I will no longer send auto-messages "
								+ "until a new channel is defined.", DiscordUtils.mentionChannel(channelId)), context.getChannel());
					}
				});
	}

	private Mono<Message> updateMessage(Context context, SettingEnum setting, Action action, List<String> args) {
		final DBGuild dbGuild = DatabaseManager.getDBGuild(context.getGuildId());
		if(Action.ENABLE.equals(action)) {
			if(args.size() < 4) {
				throw new MissingArgumentException();
			}
			final String message = args.get(3);
			dbGuild.setSetting(setting, message);

			Flux<Message> warningFlux = Flux.empty();
			if(!dbGuild.getMessageChannelId().isPresent()) {
				warningFlux = warningFlux.concatWith(BotUtils.sendMessage(String.format(Emoji.WARNING + " You need to specify a channel "
						+ "in which send the auto-messages. Use `%s%s %s %s <#channel>`",
						context.getPrefix(), this.getCommandName(),
						StringUtils.toLowerCase(Action.ENABLE), StringUtils.toLowerCase(Type.CHANNEL)), context.getChannel()));
			}

			return warningFlux.then(BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " %s set to `%s`",
					StringUtils.capitalizeEnum(setting), message), context.getChannel()));

		} else {
			dbGuild.removeSetting(setting);
			return BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " %s disabled.",
					StringUtils.capitalizeEnum(setting)), context.getChannel());
		}
	}

	@Override
	public EmbedCreateSpec getHelp(Context context) {
		return EmbedUtils.getDefaultEmbed()
				.addField("Usage", String.format("`%s%s <action> <type> [<value>]`", context.getPrefix(), this.getCommandName()), false)
				.addField("Argument", String.format("**action** - %s"
						+ "%n**type** - %s"
						+ "%n**value** - a message for *%s* and *%s* or a #channel for *%s*",
						FormatUtils.format(Action.class, "/"),
						FormatUtils.format(Type.class, "/"),
						StringUtils.toLowerCase(Type.JOIN_MESSAGE),
						StringUtils.toLowerCase(Type.LEAVE_MESSAGE),
						StringUtils.toLowerCase(Type.CHANNEL)), false)
				.addField("Info", "You don't need to specify *value* to disable a type.", false)
				.addField("Example", String.format("`%s%s enable join_message Hello you (:`"
						+ "%n`%s%s disable leave_message`",
						context.getPrefix(), this.getCommandName(), context.getPrefix(), this.getCommandName()), false);
	}
}
