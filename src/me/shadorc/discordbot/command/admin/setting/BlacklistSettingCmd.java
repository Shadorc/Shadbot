package me.shadorc.discordbot.command.admin.setting;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;

import me.shadorc.discordbot.command.CommandManager;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.data.Setting;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import sx.blah.discord.util.EmbedBuilder;

public class BlacklistSettingCmd implements SettingCmd {

	@Override
	public void execute(Context context, String arg) throws MissingArgumentException {
		if(arg == null) {
			throw new MissingArgumentException();
		}

		String[] splitArgs = StringUtils.getSplittedArg(arg, 2);
		if(splitArgs.length != 2) {
			throw new MissingArgumentException();
		}

		List<String> blacklist = Utils.convertToStringList((JSONArray) DatabaseManager.getSetting(context.getGuild(), Setting.BLACKLIST));

		String arg1 = splitArgs[0];
		String arg2 = splitArgs[1];
		switch (arg1) {
			case "add":
				List<String> commandsAdded = new ArrayList<>();
				for(String cmdName : arg2.split(",")) {
					if(CommandManager.getCommand(cmdName) == null) {
						BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " `" + cmdName + "` doesn't exist.", context.getChannel());
						continue;
					}
					if(!blacklist.contains(cmdName)) {
						blacklist.add(cmdName);
						commandsAdded.add(cmdName);
					}
				}

				if(!commandsAdded.isEmpty()) {
					BotUtils.sendMessage(Emoji.CHECK_MARK + " Command `"
							+ StringUtils.formatList(commandsAdded, cmd -> cmd, ", ")
							+ "` has been added to the blacklist.", context.getChannel());
				}
				break;

			case "remove":
				for(String name : arg2.split(",")) {
					blacklist.remove(name);
				}

				BotUtils.sendMessage(Emoji.CHECK_MARK + " Command `"
						+ StringUtils.formatArray(arg2.split(","), cmd -> cmd.toString(), ", ")
						+ "` has been removed from the blacklist.", context.getChannel());
				break;

			default:
				BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid action. Use `" + context.getPrefix() + "settings "
						+ Setting.BLACKLIST.toString() + " help` to see help.", context.getChannel());
				return;
		}
		DatabaseManager.setSetting(context.getGuild(), Setting.BLACKLIST, new JSONArray(blacklist));
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed()
				.withAuthorName("Help for setting: " + Setting.BLACKLIST.toString())
				.appendDescription("**" + this.getDescription() + "**")
				.appendField("Usage", "`" + context.getPrefix() + "settings " + Setting.BLACKLIST.toString() + " <action> <command(s)>`", false)
				.appendField("Argument", "**action** - add/remove"
						+ "\n**command(s)** - the command(s) to add/remove", false)
				.appendField("Example", "`" + context.getPrefix() + "settings " + Setting.BLACKLIST.toString() + " add rule34`", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());

	}

	@Override
	public String getDescription() {
		return "Blacklist command.";
	}

}
