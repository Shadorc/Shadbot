package me.shadorc.discordbot.command.utils;

import java.time.temporal.ChronoUnit;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.util.EmbedBuilder;

public class CalcCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public CalcCmd() {
		super(Role.USER, "calc");
		this.rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
			BotUtils.sendMessage(context.getArg().replace('*', 'x') + " = " + engine.eval(context.getArg()), context.getChannel());
		} catch (ScriptException err) {
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
