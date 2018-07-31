package me.shadorc.shadbot.command.fun;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.utils.message.VoteMessage;
import reactor.core.publisher.Mono;

@Command(category = CommandCategory.HIDDEN, names = { "test" })
public class Test extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final VoteMessage voteMessage = new VoteMessage(context.getClient(), context.getChannelId(), 10);
		return voteMessage.sendMessage(new EmbedCreateSpec().setDescription("Test"))
				.doOnSuccess(System.err::println)
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return null;
	}

}
