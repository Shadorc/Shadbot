package me.shadorc.shadbot.command.admin.setting;

import discord4j.core.object.entity.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.TextChannelEditSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.setting.AbstractSetting;
import me.shadorc.shadbot.core.setting.Setting;
import me.shadorc.shadbot.core.setting.SettingEnum;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import reactor.core.publisher.Mono;

@Setting(description = "Manage current channel's NSFW state.", setting = SettingEnum.NSFW)
public class NSFWSetting extends AbstractSetting {

	private enum Action {
		TOGGLE, ENABLE, DISABLE;
	}

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = this.requireArg(context);

		final Action action = Utils.getEnum(Action.class, arg);
		if(action == null) {
			throw new CommandException(String.format("`%s` is not a valid action. %s", arg, FormatUtils.formatOptions(Action.class)));
		}

		return context.getChannel()
				.cast(TextChannel.class)
				.flatMap(channel -> {
					boolean isNSFW = false;
					switch (action) {
						case TOGGLE:
							isNSFW = !channel.isNsfw();
							break;
						case ENABLE:
							isNSFW = true;
							break;
						case DISABLE:
							isNSFW = false;
							break;
					}

					return channel.edit(new TextChannelEditSpec().setNsfw(isNSFW))
							.then(BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " This channel is now %sSFW.", isNSFW ? "N" : ""),
									context.getChannel()));
				})
				.then();
	}

	@Override
	public EmbedCreateSpec getHelp(Context context) {
		return EmbedUtils.getDefaultEmbed()
				.addField("Usage", String.format("`%s%s <action>`", context.getPrefix(), context.getCommandName()), false)
				.addField("Argument", String.format("**action** - %s",
						FormatUtils.format(Action.class, "/")), false)
				.addField("Example", String.format("`%s%s toggle`", context.getPrefix(), context.getCommandName()), false);
	}

}
