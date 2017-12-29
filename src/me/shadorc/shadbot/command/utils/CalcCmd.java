package me.shadorc.shadbot.command.utils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;

@Command(category = CommandCategory.UTILS, names = { "calc", "math" })
public class CalcCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
			String expression = context.getArg();
			BotUtils.sendMessage(String.format("%s = %s", expression.replace("*", "\\*"), engine.eval(expression)), context.getChannel());
		} catch (ScriptException err) {
			throw new IllegalArgumentException("Invalid expression.");
		}
	}

	@Override
	public EmbedObject getHelp(Context context) {
		return new HelpBuilder(this, context.getPrefix())
				.setDescription("Calculate an expression.")
				.appendArg("expression", false)
				.build();
	}

}
