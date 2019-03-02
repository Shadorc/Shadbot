package me.shadorc.shadbot.command.music;

import java.util.List;
import java.util.function.Consumer;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

public class StopCmd extends BaseCmd {

	public StopCmd() {
		super(CommandCategory.MUSIC, List.of("stop"));
		this.setDefaultRateLimiter();
	}

	@Override
	public Mono<Void> execute(Context context) {
		return Mono.fromRunnable(() -> context.requireGuildMusic().leaveVoiceChannel())
				.then(context.getChannel())
				.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.INFO + " Music stopped by **%s**.", context.getUsername()), channel))
				.then();
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Stop music.")
				.build();
	}
}