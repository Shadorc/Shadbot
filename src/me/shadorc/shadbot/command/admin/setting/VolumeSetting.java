package me.shadorc.shadbot.command.admin.setting;

import java.util.List;
import java.util.function.Consumer;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.setting.AbstractSetting;
import me.shadorc.shadbot.core.setting.Setting;
import me.shadorc.shadbot.core.setting.SettingEnum;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@Setting(description = "Manage music default volume.", setting = SettingEnum.DEFAULT_VOLUME)
public class VolumeSetting extends AbstractSetting {

	private static final int MIN_VOLUME = 1;
	private static final int MAX_VOLUME = 75;

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(2);

		final Integer volume = NumberUtils.asIntBetween(args.get(1), MIN_VOLUME, MAX_VOLUME);
		if(volume == null) {
			throw new CommandException(String.format("`%s` is not a valid number, it must be between **%d** and **%d**.",
					args.get(1), MIN_VOLUME, MAX_VOLUME));
		}

		Shadbot.getDatabase().getDBGuild(context.getGuildId()).setSetting(this.getSetting(), volume);
		return context.getChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Default volume set to **%d%%**", volume), channel))
				.then();
	}

	@Override
	public Consumer<? super EmbedCreateSpec> getHelp(Context context) {
		return embed -> {
			EmbedUtils.getDefaultEmbed().accept(embed);
			embed.addField("Usage", String.format("`%s%s <volume>`", context.getPrefix(), this.getCommandName()), false)
				.addField("Argument", String.format("**volume** - min: %d / max: %d / default: %d",
						MIN_VOLUME, MAX_VOLUME, Config.DEFAULT_VOLUME), false)
				.addField("Example", String.format("`%s%s 42`", context.getPrefix(), this.getCommandName()), false);
		};
	}

}
