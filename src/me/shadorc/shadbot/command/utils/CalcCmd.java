package me.shadorc.shadbot.command.utils;

import com.fathzer.soft.javaluator.DoubleEvaluator;

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
import sx.blah.discord.api.internal.json.objects.EmbedObject;

@RateLimited
@Command(category = CommandCategory.UTILS, names = { "calc", "math" })
public class CalcCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		try {
			String expression = context.getArg();
			BotUtils.sendMessage(Emoji.TRIANGULAR_RULER + String.format(" %s = %s",
					expression.replace("*", "\\*"), new DoubleEvaluator().evaluate(expression)), context.getChannel());
		} catch (IllegalArgumentException err) {
			throw new IllegalCmdArgumentException(err.getMessage());
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
