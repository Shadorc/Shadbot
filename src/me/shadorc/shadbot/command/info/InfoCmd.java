package me.shadorc.shadbot.command.info;

import java.util.OptionalInt;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;

import discord4j.core.object.entity.ApplicationInfo;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.util.VersionUtil;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;

@RateLimited
@Command(category = CommandCategory.INFO, names = { "info" })
public class InfoCmd extends AbstractCommand {

	@Override
	public void execute(Context context) {
		long ping = TimeUtils.getMillisUntil(context.getMessage().getTimestamp());
		long uptime = TimeUtils.getMillisUntil(Shadbot.getLaunchTime());

		Runtime runtime = Runtime.getRuntime();
		int mbUnit = 1024 * 1024;
		long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / mbUnit;
		long maxMemory = runtime.maxMemory() / mbUnit;

		context.getClient().getApplicationInfo()
		.flatMap(ApplicationInfo::getOwner)
		.subscribe(owner -> {

			context.getClient().getGuilds()
			.map(Guild::getMemberCount)
			.collect(Collectors.summingInt(OptionalInt::getAsInt))
			.subscribe(membersCount -> {

				context.getClient()
				.getGuilds()
				.count()
				.subscribe(guildsCount -> {

					String info = new String("```prolog"
							+ String.format("%n-= Performance Info =-")
							+ String.format("%nMemory: %s/%s MB", FormatUtils.formatNum(usedMemory), FormatUtils.formatNum(maxMemory))
							+ String.format("%nCPU Usage: %.1f%%", Utils.getProcessCpuLoad())
							+ String.format("%nThreads Count: %s", FormatUtils.formatNum(Thread.activeCount()))
							+ String.format("%n%n-= APIs Info =-")
							+ String.format("%nJava Version: %s", System.getProperty("java.version"))
							 + String.format("%n%s Version: %s", VersionUtil.APPLICATION_NAME, VersionUtil.APPLICATION_VERSION)
							+ String.format("%nLavaPlayer Version: %s", PlayerLibrary.VERSION)
							+ String.format("%n%n-= Shadbot Info =-")
							+ String.format("%nUptime: %s", DurationFormatUtils.formatDuration(uptime, "d 'days,' HH 'hours and' mm 'minutes'", true))
							+ String.format("%nDeveloper: %s#%s", owner.getUsername(), owner.getDiscriminator())
							+ String.format("%nShadbot Version: %s", Config.VERSION)
							+ String.format("%nShard: %d/%d", context.getShardIndex() + 1, context.getShardCount())
							+ String.format("%nServers: %s", FormatUtils.formatNum(guildsCount))
							//TODO
							// + String.format("%nVoice Channels: %d", context.getClient().getConnectedVoiceChannels().size())
							+ String.format("%nUsers: %s", FormatUtils.formatNum(membersCount))
							+ String.format("%nPing: %dms", ping)
							+ "```");

					BotUtils.sendMessage(info, context.getChannel());
				});
			});
		});
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show Shadbot's info.")
				.build();
	}
}
