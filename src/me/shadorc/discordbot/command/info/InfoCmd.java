package me.shadorc.discordbot.command.info;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;

import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.NetUtils;
import sx.blah.discord.Discord4J;

public class InfoCmd extends Command {

	public InfoCmd() {
		super(false, "info");
	}

	@Override
	public void execute(Context context) {
		Runtime runtime = Runtime.getRuntime();

		long allocatedMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();
		long uptime = Duration.between(Discord4J.getLaunchTime().atZone(ZoneId.systemDefault()).toInstant(), Instant.now()).toMillis();
		int mb = 1024*1024;

		StringBuilder sb = new StringBuilder();
		sb.append("```prolog");
		sb.append("\n-= Performance Info =-");
		sb.append("\nMemory : " + String.format("%.1f MB / %.1f MB", (float) (allocatedMemory-freeMemory)/mb, (float) allocatedMemory/mb));
		sb.append("\nThreads Count : " + Thread.activeCount());
		sb.append("\n\n-= APIs Info =-");
		sb.append("\n" + Discord4J.NAME + " Version: " + Discord4J.VERSION);
		sb.append("\nLavaPlayer Version: " + PlayerLibrary.VERSION);
		sb.append("\n\n-= Shadbot Info =-");
		sb.append("\nUptime: " + DurationFormatUtils.formatDuration(uptime, "HH:mm:ss", true));
		sb.append("\nPing: " + NetUtils.getPing() + "ms");
		sb.append("```");

		BotUtils.sendMessage(sb.toString(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		// TODO Auto-generated method stub
	}

}
