package me.shadorc.shadbot.command.admin.setting;

import java.util.List;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.setting.AbstractSetting;
import me.shadorc.shadbot.core.setting.Setting;
import me.shadorc.shadbot.core.setting.SettingEnum;
import me.shadorc.shadbot.data.database.DatabaseManager;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@Setting(description = "Manage Shadbot's prefix.", setting = SettingEnum.PREFIX)
public class PrefixSetting extends AbstractSetting {

	private static final int MAX_PREFIX_LENGTH = 5;

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(2);

		if(args.get(1).length() > MAX_PREFIX_LENGTH) {
			throw new CommandException(String.format("Prefix cannot contain more than %s characters.", MAX_PREFIX_LENGTH));
		}

		DatabaseManager.getDBGuild(context.getGuildId()).setSetting(this.getSetting(), args.get(1));
		return BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Prefix set to `%s`", args.get(1)), context.getChannel()).then();
	}

	@Override
	public EmbedCreateSpec getHelp(Context context) {
		return EmbedUtils.getDefaultEmbed()
				.addField("Usage", String.format("`%s%s <prefix>`", context.getPrefix(), this.getCommandName()), false)
				.addField("Argument", "**prefix** - Max length: 5, must not contain spaces", false)
				.addField("Example", String.format("`%s%s !`", context.getPrefix(), this.getCommandName()), false);
	}

}
