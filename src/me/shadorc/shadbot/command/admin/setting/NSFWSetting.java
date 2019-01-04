package me.shadorc.shadbot.command.admin.setting;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import discord4j.core.object.entity.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.setting.AbstractSetting;
import me.shadorc.shadbot.core.setting.Setting;
import me.shadorc.shadbot.core.setting.SettingEnum;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@Setting(description = "Manage current channel's NSFW state.", setting = SettingEnum.NSFW)
public class NSFWSetting extends AbstractSetting {

	private enum Action {
		TOGGLE, ENABLE, DISABLE;
	}

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(2);

		final Action action = Utils.getEnum(Action.class, args.get(1));
		if(action == null) {
			throw new CommandException(String.format("`%s` is not a valid action. %s", args.get(1), FormatUtils.options(Action.class)));
		}

		return context.getChannel()
				.cast(TextChannel.class)
				.flatMap(channel -> {
					final AtomicBoolean isNSFW = new AtomicBoolean(false);
					switch (action) {
						case TOGGLE:
							isNSFW.set(!channel.isNsfw());
							break;
						case ENABLE:
							isNSFW.set(true);
							break;
						case DISABLE:
							isNSFW.set(false);
							break;
					}

					return channel.edit(spec -> spec.setNsfw(isNSFW.get()));
				})
				.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.CHECK_MARK + " (**%s**) This channel is now **%sSFW**.",
						context.getUsername(), channel.isNsfw() ? "N" : ""), channel))
				.then();
	}

	@Override
	public EmbedCreateSpec getHelp(Context context) {
		return EmbedUtils.getDefaultEmbed()
				.addField("Usage", String.format("`%s%s <action>`", context.getPrefix(), this.getCommandName()), false)
				.addField("Argument", String.format("**action** - %s",
						FormatUtils.format(Action.class, "/")), false)
				.addField("Example", String.format("`%s%s toggle`", context.getPrefix(), this.getCommandName()), false);
	}

}
