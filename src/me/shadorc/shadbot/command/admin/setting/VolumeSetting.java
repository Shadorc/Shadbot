package me.shadorc.shadbot.command.admin.setting;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.command.admin.setting.core.AbstractSetting;
import me.shadorc.shadbot.command.admin.setting.core.Setting;
import me.shadorc.shadbot.command.admin.setting.core.SettingEnum;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.CastUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.util.EmbedBuilder;

@Setting(description = "Change music default volume.", setting = SettingEnum.VOLUME)
public class VolumeSetting extends AbstractSetting {

	private static final int MIN_VOLUME = 1;
	private static final int MAX_VOLUME = 50;

	@Override
	public void execute(Context context, String arg) throws MissingArgumentException, IllegalCmdArgumentException {
		Integer volume = CastUtils.asIntBetween(arg, MIN_VOLUME, MAX_VOLUME);
		if(volume == null) {
			throw new IllegalCmdArgumentException(String.format("Invalid number, must be between %d and %d.", MIN_VOLUME, MAX_VOLUME));
		}

		Database.getDBGuild(context.getGuild()).setSetting(this.getSetting(), volume);
		BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Default volume set to **%d%**", volume), context.getChannel());
	}

	@Override
	public EmbedBuilder getHelp(String prefix) {
		return EmbedUtils.getDefaultEmbed()
				.appendField("Usage", String.format("`%s%s <volume>`", prefix, this.getCmdName()), false)
				.appendField("Argument", String.format("**volume** - min: %d / max: %d / default: %d",
						MIN_VOLUME, MAX_VOLUME, Config.DEFAULT_VOLUME), false)
				.appendField("Example", String.format("`%s%s 42`", prefix, this.getCmdName()), false);
	}

}
