package me.shadorc.shadbot.command.info;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.Command;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DateUtils;
import me.shadorc.shadbot.utils.HelpEmbedBuilder;
import sx.blah.discord.Discord4J;
import sx.blah.discord.api.internal.json.objects.EmbedObject;

@Command(category = CommandCategory.INFO, names = { "info" })
public class InfoCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		int ping = Utils.getPing(context.getMessage().getCreationDate());
		long uptime = DateUtils.getMillisUntil(Discord4J.getLaunchTime());

		Runtime runtime = Runtime.getRuntime();
		int mbUnit = 1024 * 1024;
		long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / mbUnit;
		long maxMemory = runtime.maxMemory() / mbUnit;

		String info = new String("```prolog"
				+ String.format("%n-= Performance Info =-")
				+ String.format("%nMemory: %d / %d MB", usedMemory, maxMemory)
				+ String.format("%nCPU Usage: %.1f%%", Utils.getProcessCpuLoad())
				+ String.format("%nThreads Count: %d", Thread.activeCount())
				+ String.format("%n%n-= APIs Info =-")
				+ String.format("%nJava Version: %s", System.getProperty("java.version"))
				+ String.format("%n%s Version: %s", Discord4J.NAME, Discord4J.VERSION)
				+ String.format("%nLavaPlayer Version: %s", PlayerLibrary.VERSION)
				+ String.format("%n%n-= Shadbot Info =-")
				+ String.format("%nUptime: %s", DurationFormatUtils.formatDuration(uptime, "d 'days,' HH 'hours and' mm 'minutes'", true))
				+ String.format("%nDeveloper: %s#%s", Shadbot.getOwner().getName(), Shadbot.getOwner().getDiscriminator())
				+ String.format("%nShadbot Version: %s", Config.VERSION)
				+ String.format("%nShard: %d / %d", context.getShadbotShard().getID() + 1, Shadbot.getClient().getShardCount())
				+ String.format("%nServers: %d", Shadbot.getClient().getGuilds().size())
				+ String.format("%nVoice Channels: %d", Shadbot.getClient().getConnectedVoiceChannels().size())
				+ String.format("%nUsers: %d", Shadbot.getClient().getUsers().size())
				+ String.format("%nPing: %d ms", ping)
				+ "```");

		BotUtils.sendMessage(info, context.getChannel());
	}

	@Override
	public EmbedObject getHelp(Context context) {
		return new HelpEmbedBuilder(this, context.getPrefix())
				.setDescription("Show Shadbot's info.")
				.build();
	}
}
