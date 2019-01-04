package me.shadorc.shadbot.command.hidden;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.HIDDEN, names = { "baguette" })
public class BaguetteCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed()
				.setImage("http://i.telegraph.co.uk/multimedia/archive/02600/CECPY7_2600591b.jpg");
		return context.getChannel().flatMap(channel -> DiscordUtils.sendMessage(embed, channel)).then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("This command doesn't exist.")
				.build();
	}

	// Essential part of Shadbot (Thanks to @Bluerin)
	public String howToDoAChocolateCake() {
		final String meal = "50g farine";
		final String chocolate = "200g chocolat";
		final String eggs = "3 oeufs";
		final String sugar = "100g sucre";
		final String butter = "100g beurre";
		return "Mélanger " + meal + " " + sugar + " " + eggs + " dans un saladier." +
				"\nFaire fondre au bain-marie " + chocolate + butter +
				"\nRajouter le chocolat et le beurre dans le saladier.\nVerser le mélange dans un moule et enfourner pendant 25min à 180°C.";
	}

}
