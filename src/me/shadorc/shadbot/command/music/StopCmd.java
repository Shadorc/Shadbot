package me.shadorc.shadbot.command.music;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.MUSIC, names = { "stop" })
public class StopCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		return Mono.fromRunnable(() -> context.requireGuildMusic().leaveVoiceChannel())
				.then(context.getChannel())
				.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.INFO + " Music stopped by **%s**.", context.getUsername()), channel))
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Stop music.")
				.build();
	}
}