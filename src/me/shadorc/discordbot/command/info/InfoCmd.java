package me.shadorc.discordbot.command.info;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.Discord4J;
import sx.blah.discord.util.EmbedBuilder;

public class InfoCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public InfoCmd() {
		super(CommandCategory.INFO, Role.USER, "info");
		this.rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		long ping = Math.abs(ChronoUnit.MILLIS.between(LocalDateTime.now(), context.getMessage().getCreationDate()));

		Runtime runtime = Runtime.getRuntime();
		long usedMemory = runtime.totalMemory() - runtime.freeMemory();
		long maxMemory = runtime.maxMemory();
		long uptime = Duration.between(Discord4J.getLaunchTime().atZone(ZoneId.systemDefault()).toInstant(), Instant.now()).toMillis();
		int mbUnit = 1024 * 1024;

		String info = new String(
				"```prolog"
						+ "\n-= Performance Info =-"
						+ "\nMemory: " + String.format("%d MB / %d MB", usedMemory / mbUnit, maxMemory / mbUnit)
						+ "\nCPU Usage: " + String.format("%.1f", Utils.getProcessCpuLoad()) + "%"
						+ "\nThreads Count: " + Thread.activeCount()
						+ "\n\n-= APIs Info =-"
						+ "\nJava Version: " + System.getProperty("java.version")
						+ "\n" + Discord4J.NAME + " Version: " + Discord4J.VERSION
						+ "\nLavaPlayer Version: " + PlayerLibrary.VERSION
						+ "\n\n-= Shadbot Info =-"
						+ "\nUptime: " + DurationFormatUtils.formatDuration(uptime, "d 'days,' HH 'hours and' mm 'minutes'", true)
						+ "\nDeveloper: " + Shadbot.getOwner().getName() + "#" + Shadbot.getOwner().getDiscriminator()
						+ "\nVersion: " + Config.VERSION.toString()
						+ "\nVoice Channels: " + Shadbot.getClient().getConnectedVoiceChannels().size()
						+ "\nServers: " + Shadbot.getClient().getGuilds().size()
						+ "\nUsers: " + Shadbot.getClient().getUsers().size()
						+ "\nPing: " + ping + "ms"
						+ "```");

		BotUtils.sendMessage(info, context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show Shadbot's info.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
