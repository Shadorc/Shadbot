package me.shadorc.shadbot.command.utils;

import com.fathzer.soft.javaluator.DoubleEvaluator;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import reactor.core.publisher.Mono;

import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Consumer;

public class CalcCmd extends BaseCmd {

	private final DoubleEvaluator evaluator;
	private final DecimalFormat formatter;

	public CalcCmd() {
		super(CommandCategory.UTILS, List.of("calc", "math"));
		this.setDefaultRateLimiter();

		this.evaluator = new DoubleEvaluator();
		this.formatter = new DecimalFormat("#.##");
	}

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		return context.getChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.TRIANGULAR_RULER + " (**%s**) %s = %s",
						context.getUsername(), arg.replace("*", "\\*"), this.formatter.format(this.evaluator.evaluate(arg))),
						channel))
				.onErrorMap(IllegalArgumentException.class, err -> new CommandException(err.getMessage()))
				.then();
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Calculate an expression.")
				.addArg("expression", false)
				.setExample(String.format("`%s%s 3+3*3+3`%n`%s%s 2*cos(pi)`",
						context.getPrefix(), this.getName(), context.getPrefix(), this.getName()))
				.build();
	}

}
