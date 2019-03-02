package me.shadorc.shadbot.command.fun;

import java.util.List;
import java.util.function.Consumer;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import reactor.core.publisher.Mono;

public class LeetCmd extends BaseCmd {

	public LeetCmd() {
		super(CommandCategory.FUN, List.of("leet"));
		this.setDefaultRateLimiter();
	}

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

		final Consumer<EmbedCreateSpec> embedConsumer = EmbedUtils.getDefaultEmbed()
				.andThen(embed -> embed.setAuthor("Leetifier", null, context.getAvatarUrl())
						.setDescription(String.format("**Original**%n%s%n%n**Leetified**%n%s", arg, text)));

		return context.getChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(embedConsumer, channel))
				.then();
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Leetify a text.")
				.addArg("text", false)
				.build();
	}

}
