package me.shadorc.shadbot.command.info;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;

import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DateUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.Discord4J;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IUser;

@RateLimited
@Command(category = CommandCategory.INFO, names = { "info" })
public class InfoCmd extends AbstractCommand {

	@Override
	public void execute(Context context) {
		long ping = DateUtils.getMillisUntil(context.getMessage().getCreationDate());
		long uptime = DateUtils.getMillisUntil(Discord4J.getLaunchTime());

		Runtime runtime = Runtime.getRuntime();
		int mbUnit = 1024 * 1024;
		long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / mbUnit;
		long maxMemory = runtime.maxMemory() / mbUnit;

		IUser owner = context.getClient().getApplicationOwner();

		String info = new String("```prolog"
				+ String.format("%n-= Performance Info =-")
				+ String.format("%nMemory: %s/%s MB", FormatUtils.formatNum(usedMemory), FormatUtils.formatNum(maxMemory))
				+ String.format("%nCPU Usage: %.1f%%", Utils.getProcessCpuLoad())
				+ String.format("%nThreads Count: %s", FormatUtils.formatNum(Thread.activeCount()))
				+ String.format("%n%n-= APIs Info =-")
				+ String.format("%nJava Version: %s", System.getProperty("java.version"))
				+ String.format("%n%s Version: %s", Discord4J.NAME, Discord4J.VERSION)
				+ String.format("%nLavaPlayer Version: %s", PlayerLibrary.VERSION)
				+ String.format("%n%n-= Shadbot Info =-")
				+ String.format("%nUptime: %s", DurationFormatUtils.formatDuration(uptime, "d 'days,' HH 'hours and' mm 'minutes'", true))
				+ String.format("%nDeveloper: %s#%s", owner.getName(), owner.getDiscriminator())
				+ String.format("%nShadbot Version: %s", Shadbot.getVersion())
				+ String.format("%nShard: %d/%d", context.getShadbotShard().getID() + 1, context.getClient().getShardCount())
				+ String.format("%nServers: %s", FormatUtils.formatNum(context.getClient().getGuilds().size()))
				+ String.format("%nVoice Channels: %d", context.getClient().getConnectedVoiceChannels().size())
				+ String.format("%nUsers: %s", FormatUtils.formatNum(context.getClient().getUsers().size()))
				+ String.format("%nPing: %dms", ping)
				+ "```");

		BotUtils.sendMessage(info, context.getChannel());
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show Shadbot's info.")
				.build();
	}
}
