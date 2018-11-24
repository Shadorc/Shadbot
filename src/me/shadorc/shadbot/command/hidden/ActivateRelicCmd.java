package me.shadorc.shadbot.command.hidden;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.exception.RelicActivationException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@Command(category = CommandCategory.HIDDEN, names = { "activate_relic", "activate" })
public class ActivateRelicCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		return context.getGuild()
				.flatMap(guild -> {
					try {
						Shadbot.getPremium().activateRelic(guild.getId(), context.getAuthorId(), arg);
						return BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " (**%s**) Relic successfully activated, enjoy !",
								context.getUsername()), context.getChannel());
					} catch (RelicActivationException err) {
						return BotUtils.sendMessage(String.format(Emoji.GREY_EXCLAMATION + " (**%s**) %s",
								context.getUsername(), err.getMessage()), context.getChannel());
					}
				})
				.then();

	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Activate a relic.")
				.addArg("key", false)
				.build();
	}
}
