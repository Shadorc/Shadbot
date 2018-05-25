package me.shadorc.shadbot.command.admin.setting;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.command.admin.setting.core.AbstractSetting;
import me.shadorc.shadbot.command.admin.setting.core.Setting;
import me.shadorc.shadbot.command.admin.setting.core.SettingEnum;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.Emoji;

@Setting(description = "Manage music default volume.", setting = SettingEnum.DEFAULT_VOLUME)
public class VolumeSetting extends AbstractSetting {

	private static final int MIN_VOLUME = 1;
	private static final int MAX_VOLUME = 75;

	@Override
	public void execute(Context context, String arg) throws MissingArgumentException, IllegalCmdArgumentException {
		context.requireArg();

		Integer volume = NumberUtils.asIntBetween(arg, MIN_VOLUME, MAX_VOLUME);
		if(volume == null) {
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid number, it must be between %d and %d.",
					arg, MIN_VOLUME, MAX_VOLUME));
		}

		Database.getDBGuild(context.getGuildId().get()).setSetting(this.getSetting(), volume);
		BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Default volume set to **%d%%**", volume), context.getChannel());
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		return EmbedUtils.getDefaultEmbed()
				.addField("Usage", String.format("`%s%s <volume>`", prefix, this.getCmdName()), false)
				.addField("Argument", String.format("**volume** - min: %d / max: %d / default: %d",
						MIN_VOLUME, MAX_VOLUME, Config.DEFAULT_VOLUME), false)
				.addField("Example", String.format("`%s%s 42`", prefix, this.getCmdName()), false);
	}

}
