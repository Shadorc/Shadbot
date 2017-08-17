package me.shadorc.discordbot.command.info;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.Discord4J;
import sx.blah.discord.util.EmbedBuilder;

public class InfoCmd extends AbstractCommand {

	public InfoCmd() {
		super(false, "info");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		Runtime runtime = Runtime.getRuntime();

		long usedMemory = runtime.totalMemory() - runtime.freeMemory();
		long maxMemory = runtime.maxMemory();
		long uptime = Duration.between(Discord4J.getLaunchTime().atZone(ZoneId.systemDefault()).toInstant(), Instant.now()).toMillis();
		int mbUnit = 1024 * 1024;

		String info = new String(
				"```prolog"
						+ "\n-= Performance Info =-"
						+ "\nMemory : " + String.format("%d MB / %d MB", usedMemory / mbUnit, maxMemory / mbUnit)
						+ "\nCPU Usage : " + String.format("%.1f", Utils.getProcessCpuLoad()) + "%"
						+ "\nThreads Count : " + Thread.activeCount()
						+ "\n\n-= APIs Info =-"
						+ "\nJava Version: " + System.getProperty("java.version")
						+ "\n" + Discord4J.NAME + " Version: " + Discord4J.VERSION
						+ "\nLavaPlayer Version: " + PlayerLibrary.VERSION
						+ "\n\n-= Shadbot Info =-"
						+ "\nUptime: " + DurationFormatUtils.formatDuration(uptime, "d 'days,' HH 'hours and' mm 'minutes'", true)
						+ "\nVersion: " + Config.VERSION.toString()
						+ "\nDeveloper: Shadorc#8423"
						+ "\nServers: " + Shadbot.getClient().getGuilds().size()
						+ "\nVoice Channel(s): " + Shadbot.getClient().getConnectedVoiceChannels().size()
						+ "\nUsers: " + Shadbot.getClient().getUsers().size()
						+ "\nPing: " + NetUtils.getPing() + "ms"
						+ "```");

		BotUtils.sendMessage(info, context.getChannel());
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
