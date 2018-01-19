package me.shadorc.shadbot.command.admin.setting;

import me.shadorc.shadbot.command.admin.setting.core.AbstractSetting;
import me.shadorc.shadbot.command.admin.setting.core.Setting;
import me.shadorc.shadbot.command.admin.setting.core.SettingEnum;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.util.EmbedBuilder;

@Setting(description = "Manage Shadbot's prefix.", setting = SettingEnum.PREFIX)
public class PrefixSetting extends AbstractSetting {

	private static final int MAX_PREFIX_LENGTH = 5;

	@Override
	public void execute(Context context, String arg) throws MissingArgumentException, IllegalCmdArgumentException {
		if(arg == null) {
			throw new MissingArgumentException();
		}

		if(arg.length() > MAX_PREFIX_LENGTH) {
			throw new IllegalCmdArgumentException(String.format("Prefix cannot contain more than %s characters.", MAX_PREFIX_LENGTH));
		}

		Database.getDBGuild(context.getGuild()).setSetting(this.getSetting(), arg);
		BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Prefix set to `%s`", arg), context.getChannel());
	}

	@Override
	public EmbedBuilder getHelp(String prefix) {
		return EmbedUtils.getDefaultEmbed()
				.appendField("Usage", String.format("`%s%s <prefix>`", prefix, this.getCmdName()), false)
				.appendField("Argument", "**prefix** - Max length: 5, must not contain spaces", false)
				.appendField("Example", String.format("`%s%s !`", prefix, this.getCmdName()), false);
	}

}
