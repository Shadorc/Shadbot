package me.shadorc.shadbot.command.info;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.INFO, names = { "ping" })
public class PingCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		return BotUtils.sendMessage(String.format(Emoji.GEAR + " Ping: %dms",
				TimeUtils.getMillisUntil(context.getMessage().getTimestamp())), context.getChannel())
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show Shadbot's ping.")
				.build();
	}
}
