package me.shadorc.shadbot.command.fun;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.FUN, names = { "leet" })
public class LeetCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		final String text = arg.toUpperCase()
				.replace("A", "4")
				.replace("B", "8")
				.replace("E", "3")
				.replace("G", "6")
				.replace("L", "1")
				.replace("O", "0")
				.replace("S", "5")
				.replace("T", "7");

		return BotUtils.sendMessage(Emoji.KEYBOARD + " " + text, context.getChannel()).then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Leetify a text.")
				.addArg("text", false)
				.build();
	}

}
