package me.shadorc.shadbot.command.admin.setting;

import java.util.List;
import java.util.function.Consumer;

import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Permission;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.setting.BaseSetting;
import me.shadorc.shadbot.core.setting.Setting;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

public class NSFWSetting extends BaseSetting {

	private enum Action {
		TOGGLE, ENABLE, DISABLE;
	}

	public NSFWSetting() {
		super(Setting.NSFW, "Manage current channel's NSFW state.");
	}

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(2);

		final Action action = Utils.parseEnum(Action.class, args.get(1));
		if(action == null) {
			return Mono.error(new CommandException(String.format("`%s` is not a valid action. %s",
					args.get(1), FormatUtils.options(Action.class))));
		}

		return context.getChannel()
				.cast(TextChannel.class)
				.flatMap(channel -> DiscordUtils.requirePermissions(channel, Permission.MANAGE_CHANNELS)
						.then(Mono.fromSupplier(() -> {
							switch (action) {
								case TOGGLE:
									return !channel.isNsfw();
								case ENABLE:
									return true;
								default:
									return false;
							}
						}))
						.flatMap(nsfw -> channel.edit(spec -> spec.setNsfw(nsfw))))
				.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.CHECK_MARK + " (**%s**) %s is now **%sSFW**.",
						context.getUsername(), channel.getMention(), channel.isNsfw() ? "N" : ""), channel))
				.then();
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return EmbedUtils.getDefaultEmbed()
				.andThen(embed -> embed.addField("Usage", String.format("`%s%s <action>`", context.getPrefix(), this.getCommandName()), false)
						.addField("Argument", String.format("**action** - %s",
								FormatUtils.format(Action.class, "/")), false)
						.addField("Example", String.format("`%s%s toggle`", context.getPrefix(), this.getCommandName()), false));
	}

}
