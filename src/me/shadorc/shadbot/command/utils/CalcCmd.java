package me.shadorc.shadbot.command.utils;

import com.fathzer.soft.javaluator.DoubleEvaluator;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.UTILS, names = { "calc", "math" })
public class CalcCmd extends AbstractCommand {

	private static final DoubleEvaluator EVALUATOR = new DoubleEvaluator();

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		try {
			return BotUtils.sendMessage(String.format(Emoji.TRIANGULAR_RULER + " (**%s**) %s = %s",
					context.getUsername(), arg.replace("*", "\\*"), EVALUATOR.evaluate(arg)),
					context.getChannel())
					.then();
		} catch (IllegalArgumentException err) {
			throw new CommandException(err.getMessage());
		}
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Calculate an expression.")
				.addArg("expression", false)
				.setExample(String.format("`%s%s 3+3*3+3`", context.getPrefix(), this.getName()))
				.build();
	}

}
