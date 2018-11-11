package me.shadorc.shadbot.command.info;

import java.util.List;
import java.util.OptionalInt;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;

import discord4j.core.DiscordClient;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.ApplicationInfo;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
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
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.INFO, names = { "info" })
public class InfoCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final long start = System.currentTimeMillis();
		final long uptime = TimeUtils.getMillisUntil(Shadbot.getLaunchTime());

		final Runtime runtime = Runtime.getRuntime();
		final int mbUnit = 1024 * 1024;
		final long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / mbUnit;
		final long maxMemory = runtime.maxMemory() / mbUnit;

		final String d4jName = VersionUtil.getProperties().getProperty(VersionUtil.APPLICATION_NAME);
		final String d4jVersion = VersionUtil.getProperties().getProperty(VersionUtil.APPLICATION_VERSION);

		final Mono<Long> voiceChannelsCountMono = Flux.fromIterable(Shadbot.getClients())
				.flatMap(DiscordClient::getGuilds)
				.flatMap(guild -> guild.getMemberById(context.getSelfId()))
				.flatMap(Member::getVoiceState)
				.flatMap(VoiceState::getChannel)
				.count();

		return Mono.zip(context.getClient().getApplicationInfo().flatMap(ApplicationInfo::getOwner),
				Flux.fromIterable(Shadbot.getClients()).flatMap(DiscordClient::getGuilds).collectList(),
				voiceChannelsCountMono)
				.map(tuple -> {
					final User owner = tuple.getT1();
					final List<Guild> guilds = tuple.getT2();
					final Long voiceChannelsCount = tuple.getT3();

					// TODO: This needs to be unique users
					final int membersCount = guilds.stream()
							.map(Guild::getMemberCount)
							.mapToInt(OptionalInt::getAsInt)
							.sum();

					return new String("```prolog"
							+ String.format("%n-= Performance Info =-")
							+ String.format("%nMemory: %s/%s MB", FormatUtils.number(usedMemory), FormatUtils.number(maxMemory))
							+ String.format("%nCPU Usage: %.1f%%", Utils.getProcessCpuLoad())
							+ String.format("%nThreads Count: %s", FormatUtils.number(Thread.activeCount()))
							+ String.format("%n%n-= APIs Info =-")
							+ String.format("%nJava Version: %s", System.getProperty("java.version"))
							+ String.format("%n%s Version: %s", d4jName, d4jVersion)
							+ String.format("%nLavaPlayer Version: %s", PlayerLibrary.VERSION)
							+ String.format("%n%n-= Shadbot Info =-")
							+ String.format("%nUptime: %s", DurationFormatUtils.formatDuration(uptime, "d 'days,' HH 'hours and' mm 'minutes'", true))
							+ String.format("%nDeveloper: %s#%s", owner.getUsername(), owner.getDiscriminator())
							+ String.format("%nShadbot Version: %s", Config.VERSION)
							+ String.format("%nShard: %d/%d", context.getShardIndex() + 1, context.getShardCount())
							+ String.format("%nServers: %s", FormatUtils.number(guilds.size()))
							+ String.format("%nVoice Channels: %d", voiceChannelsCount)
							+ String.format("%nUsers: %s", FormatUtils.number(membersCount))
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
