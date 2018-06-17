package me.shadorc.shadbot.command.utils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.UTILS, names = { "calc", "math" })
public class CalcCmd extends AbstractCommand {

	@Override
	public void execute(Context context) {
		context.requireArg();

		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
			String expression = context.getArg().get();
			BotUtils.sendMessage(Emoji.TRIANGULAR_RULER + String.format(" %s = %s",
					expression.replace("*", "\\*"), engine.eval(expression)), context.getChannel());
		} catch (ScriptException err) {
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid expression.", context.getArg()));
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
