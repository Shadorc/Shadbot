package me.shadorc.discordbot.command.info;

import java.time.ZoneId;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;

public class PingCmd extends Command {

	public PingCmd() {
		super(false, "ping");
	}

	@Override
	public void execute(Context context) {
		long messageMillisTime = context.getMessage().getCreationDate().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		long currentMillisTime = System.currentTimeMillis();
		long ping = currentMillisTime - messageMillisTime;
		BotUtils.sendMessage(Emoji.GEAR + " Ping : " + ping + "ms", context.getChannel());
	}

}
