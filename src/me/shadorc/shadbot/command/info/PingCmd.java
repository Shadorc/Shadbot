package me.shadorc.shadbot.command.info;

import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.shadbot.command.Command;
import me.shadorc.shadbot.command.Context;
import me.shadorc.shadbot.utils.DateUtils;

public class PingCmd {

	@Command(names = { "ping" })
	public static void ping(Context context) {
		BotUtils.sendMessage(Emoji.GEAR + " Ping: " + DateUtils.getMillisUntil(context.getMessage().getCreationDate()) + "ms", context.getChannel());
	}

}
