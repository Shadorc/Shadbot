package me.shadorc.shadbot.command.utils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;

@RateLimited
@Command(category = CommandCategory.UTILS, names = { "calc", "math" })
public class CalcCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
			String expression = context.getArg();
			BotUtils.sendMessage(Emoji.TRIANGULAR_RULER + String.format(" %s = %s",
					expression.replace("*", "\\*"), engine.eval(expression)), context.getChannel());
		} catch (ScriptException err) {
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid expression.", context.getArg()));
		}
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Calculate an expression.")
				.addArg("expression", false)
				.setExample(String.format("`%s%s 3+3*3+3`", prefix, this.getName()))
				.build();
	}

}
