package me.shadorc.discordbot.command.utility;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;

public class CalcCmd extends Command {

	public CalcCmd() {
		super(false, "calc");
	}

	@Override
	public void execute(Context context) {
		if(context.getArg() == null) {
			BotUtils.sendMessage("Merci d'entrer un calcul.", context.getChannel());
			return;
		}

		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
			BotUtils.sendMessage(context.getArg() + " = " + engine.eval(context.getArg()), context.getChannel());
		} catch (ScriptException e) {
			BotUtils.sendMessage("Calcul incorrect.", context.getChannel());
		}
	}

}
