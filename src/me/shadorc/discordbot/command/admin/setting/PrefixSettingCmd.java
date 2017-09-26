package me.shadorc.discordbot.command.admin.setting;

import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.Storage;
import me.shadorc.discordbot.data.Storage.Setting;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import sx.blah.discord.util.EmbedBuilder;

public class PrefixSettingCmd implements SettingCmd {

	private static final int PREFIX_MAX_LENGTH = 5;

	@Override
	public void execute(Context context, String arg) throws MissingArgumentException {
		if(arg == null) {
			throw new MissingArgumentException();
		}

		if(arg.contains(" ")) {
			BotUtils.send(Emoji.GREY_EXCLAMATION + " Prefix cannot contain space.", context.getChannel());
			return;
		}

		if(arg.length() > PREFIX_MAX_LENGTH) {
			BotUtils.send(Emoji.GREY_EXCLAMATION + " Prefix cannot contain more than " + PREFIX_MAX_LENGTH + " characters.", context.getChannel());
			return;
		}

		Storage.saveSetting(context.getGuild(), Setting.PREFIX, arg);
		BotUtils.send(Emoji.CHECK_MARK + " '" + arg + "' is now the prefix for this server.", context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed()
				.withAuthorName("Help for setting: " + Setting.PREFIX.toString())
				.appendDescription("**" + this.getDescription() + "**")
				.appendField("Usage", "`" + context.getPrefix() + "settings " + Setting.PREFIX.toString() + " <prefix>`", false)
				.appendField("Argument", "**prefix** - Max length: 5, must not contain spaces", false)
				.appendField("Example", "`" + context.getPrefix() + "settings " + Setting.PREFIX.toString() + " !`", false);
		BotUtils.send(builder.build(), context.getChannel());
	}

	@Override
	public String getDescription() {
		return "Change Shadbot's prefix.";
	}
}
