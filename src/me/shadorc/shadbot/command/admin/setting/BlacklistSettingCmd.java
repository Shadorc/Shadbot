package me.shadorc.shadbot.command.admin.setting;

import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;

import me.shadorc.shadbot.command.admin.setting.core.AbstractSetting;
import me.shadorc.shadbot.command.admin.setting.core.Setting;
import me.shadorc.shadbot.command.admin.setting.core.SettingEnum;
import me.shadorc.shadbot.core.command.CommandManager;
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
import sx.blah.discord.util.EmbedBuilder;

@Setting(description = "Blacklist command.", setting = SettingEnum.BLACKLIST)
public class BlacklistSettingCmd extends AbstractSetting {

	private enum Action {
		ADD, REMOVE;
	}

	@Override
	public void execute(Context context, String arg) throws MissingArgumentException, IllegalCmdArgumentException {
		List<String> splitArgs = StringUtils.split(arg, 2);
		if(splitArgs.size() != 2) {
			throw new MissingArgumentException();
		}

		Action action = Utils.getValueOrNull(Action.class, splitArgs.get(0));
		if(action == null) {
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid action. Options: %s",
					splitArgs.get(0), FormatUtils.format(Action.values(), value -> value.toString().toLowerCase(), ", ")));
		}

		List<String> blacklist = Database.getDBGuild(context.getGuild()).getBlacklistedCmd();
		List<String> commands = StringUtils.split(splitArgs.get(1).toLowerCase());
		if(Action.ADD.equals(action)) {
			if(commands.stream().anyMatch(cmd -> CommandManager.getCommand(cmd) == null)) {
				throw new IllegalCmdArgumentException(String.format(" `%s` doesn't exist.",
						commands.stream().filter(cmd -> CommandManager.getCommand(cmd) == null).collect(Collectors.joining(", "))));
			}

			blacklist.addAll(commands);
			BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " `%s` added to the blacklist.",
					FormatUtils.format(commands, Object::toString, ", ")), context.getChannel());
		} else if(Action.REMOVE.equals(action)) {
			blacklist.removeAll(commands);
			BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " `%s` removed from the blacklist.",
					FormatUtils.format(commands, Object::toString, ", ")), context.getChannel());
		}

		Database.getDBGuild(context.getGuild()).setSetting(this.getSetting(), new JSONArray(blacklist));
	}

	@Override
	public EmbedBuilder getHelp(String prefix) {
		return EmbedUtils.getDefaultEmbed()
				.appendField("Usage", String.format("`%s%s <action> <command(s)>`", prefix, this.getCmdName()), false)
				.appendField("Argument", String.format("**action** - %s",
						FormatUtils.format(Action.values(), action -> action.toString().toLowerCase(), "/")), false)
				.appendField("Example", String.format("`%s%s add rule34 russian_roulette`", prefix, this.getCmdName()), false);
	}

}
