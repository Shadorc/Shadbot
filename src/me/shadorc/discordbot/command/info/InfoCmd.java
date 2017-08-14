package me.shadorc.discordbot.command.info;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.NetUtils;
import sx.blah.discord.Discord4J;
import sx.blah.discord.util.EmbedBuilder;

public class InfoCmd extends Command {

	public InfoCmd() {
		super(false, "info");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		Runtime runtime = Runtime.getRuntime();

		long usedMemory = runtime.totalMemory() - runtime.freeMemory();
		long maxMemory = runtime.maxMemory();
		long uptime = Duration.between(Discord4J.getLaunchTime().atZone(ZoneId.systemDefault()).toInstant(), Instant.now()).toMillis();
		int mb = 1024 * 1024;

		StringBuilder sb = new StringBuilder();
		sb.append("```prolog");
		sb.append("\n-= Performance Info =-");
		sb.append("\nMemory : " + String.format("%d MB / %d MB", usedMemory / mb, maxMemory / mb));
		sb.append("\nThreads Count : " + Thread.activeCount());
		sb.append("\n\n-= APIs Info =-");
		sb.append("\nJava Version: " + System.getProperty("java.version"));
		sb.append("\n" + Discord4J.NAME + " Version: " + Discord4J.VERSION);
		sb.append("\nLavaPlayer Version: " + PlayerLibrary.VERSION);
		sb.append("\n\n-= Shadbot Info =-");
		sb.append("\nDeveloper: Shadorc#8423");
		sb.append("\nServers: " + Shadbot.getClient().getGuilds().size());
		sb.append("\nUsers: " + Shadbot.getClient().getUsers().size());
		sb.append("\nVersion: " + Config.VERSION.toString());
		sb.append("\nUptime: " + DurationFormatUtils.formatDuration(uptime, "d 'days,' HH 'hours and' mm 'minutes'", true));
		sb.append("\nPing: " + NetUtils.getPing() + "ms");
		sb.append("```");

		BotUtils.sendMessage(sb.toString(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show Shadbot's info.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
