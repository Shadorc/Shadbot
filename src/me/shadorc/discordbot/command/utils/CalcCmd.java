package me.shadorc.discordbot.command.utils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.util.EmbedBuilder;

public class CalcCmd extends AbstractCommand {

	public CalcCmd() {
		super(Role.USER, "calc");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
			BotUtils.sendMessage(context.getArg() + " = " + engine.eval(context.getArg()), context.getChannel());
		} catch (ScriptException err) {
			LogUtils.info("{CalcCmd} {Guild: " + context.getGuild().getName() + " (ID: " + context.getGuild().getStringID() + ")} Invalid expression: " + context.getArg() + ".");
			BotUtils.sendMessage(Emoji.EXCLAMATION + " Invalid expression.", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Calculate an expression.**")
				.appendField("Usage", context.getPrefix() + "calc <expression>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
