package me.shadorc.shadbot.command.info;

import me.shadorc.discordbot.exceptions.MissingArgumentException;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.SubCommand;
import me.shadorc.shadbot.utils.DateUtils;

@Command(names = { "ping" }, category = CommandCategory.INFO)
public class PingCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		BotUtils.sendMessage(String.format(Emoji.GEAR + " Ping: %ld ms",
				DateUtils.getMillisUntil(context.getMessage().getCreationDate())), context.getChannel());
	}

	@SubCommand(names = { "pong" })
	public void subPing(Context context) {
		System.err.println("Pong");
	}

	@Override
	public void help(Context context) {
		// TODO Auto-generated method stub
	}
}
