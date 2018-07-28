package me.shadorc.shadbot.command.info;

import java.util.List;
import java.util.OptionalInt;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;

import discord4j.core.object.entity.ApplicationInfo;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
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
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.INFO, names = { "info" })
public class InfoCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final long start = System.currentTimeMillis();
		final long uptime = TimeUtils.getMillisUntil(Shadbot.getLaunchTime());

		Runtime runtime = Runtime.getRuntime();
		final int mbUnit = 1024 * 1024;
		final long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / mbUnit;
		final long maxMemory = runtime.maxMemory() / mbUnit;

		final String d4jName = VersionUtil.getProperties().getProperty(VersionUtil.APPLICATION_NAME);
		final String d4jVersion = VersionUtil.getProperties().getProperty(VersionUtil.APPLICATION_VERSION);

		return Mono.zip(context.getClient().getApplicationInfo().flatMap(ApplicationInfo::getOwner),
				context.getClient().getGuilds().collectList(),
				DiscordUtils.getConnectedVoiceChannelCount(context.getClient()))
				.map(tuple3 -> {
					final User owner = tuple3.getT1();
					final List<Guild> guilds = tuple3.getT2();
					final Long connectedVoiceChannels = tuple3.getT3();

					final int membersCount = guilds.stream()
							.map(Guild::getMemberCount)
							.map(OptionalInt::getAsInt)
							.mapToInt(Integer::intValue)
							.sum();

					return new String("```prolog"
							+ String.format("%n-= Performance Info =-")
							+ String.format("%nMemory: %s/%s MB", FormatUtils.formatNum(usedMemory), FormatUtils.formatNum(maxMemory))
							+ String.format("%nCPU Usage: %.1f%%", Utils.getProcessCpuLoad())
							+ String.format("%nThreads Count: %s", FormatUtils.formatNum(Thread.activeCount()))
							+ String.format("%n%n-= APIs Info =-")
							+ String.format("%nJava Version: %s", System.getProperty("java.version"))
							+ String.format("%n%s Version: %s", d4jName, d4jVersion)
							+ String.format("%nLavaPlayer Version: %s", PlayerLibrary.VERSION)
							+ String.format("%n%n-= Shadbot Info =-")
							+ String.format("%nUptime: %s", DurationFormatUtils.formatDuration(uptime, "d 'days,' HH 'hours and' mm 'minutes'", true))
							+ String.format("%nDeveloper: %s#%s", owner.getUsername(), owner.getDiscriminator())
							+ String.format("%nShadbot Version: %s", Config.VERSION)
							+ String.format("%nShard: %d/%d", context.getShardIndex() + 1, context.getShardCount())
							+ String.format("%nServers: %s", FormatUtils.formatNum(guilds.size()))
							+ String.format("%nVoice Channels: %d", connectedVoiceChannels)
							+ String.format("%nUsers: %s", FormatUtils.formatNum(membersCount))
							+ String.format("%nPing: %dms", TimeUtils.getMillisUntil(start))
							+ "```");
				})
				.flatMap(info -> BotUtils.sendMessage(info, context.getChannel()))
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show Shadbot's info.")
				.build();
	}
}
