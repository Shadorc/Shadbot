package me.shadorc.shadbot.command.info;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;

import discord4j.core.DiscordClient;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.ApplicationInfo;
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
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.INFO, names = { "info" })
public class InfoCmd extends AbstractCommand {

	private static final int MB_UNIT = 1024 * 1024;
	private static final String D4J_NAME = VersionUtil.getProperties().getProperty(VersionUtil.APPLICATION_NAME);
	private static final String D4J_VERSION = VersionUtil.getProperties().getProperty(VersionUtil.APPLICATION_VERSION);

	@Override
	public Mono<Void> execute(Context context) {
		final long uptime = TimeUtils.getMillisUntil(Shadbot.getLaunchTime());

		final Runtime runtime = Runtime.getRuntime();
		final long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / MB_UNIT;
		final long maxMemory = runtime.maxMemory() / MB_UNIT;

		final Mono<Long> voiceChannelCountMono = Flux.fromIterable(Shadbot.getClients())
				.flatMap(DiscordClient::getGuilds)
				.flatMap(guild -> guild.getMemberById(context.getSelfId()))
				.flatMap(Member::getVoiceState)
				.flatMap(VoiceState::getChannel)
				.count();

		final Mono<Long> guildCountMono = Flux.fromIterable(Shadbot.getClients())
				.flatMap(DiscordClient::getGuilds)
				.count();

		final Mono<Long> memberCountMono = Flux.fromIterable(Shadbot.getClients())
				.flatMap(DiscordClient::getUsers)
				.count();

		final long start = System.currentTimeMillis();
		return context.getChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.GEAR + " (**%s**) Loading info...", context.getUsername()), channel))
				.flatMap(message -> Mono.zip(context.getClient().getApplicationInfo().flatMap(ApplicationInfo::getOwner),
						guildCountMono, voiceChannelCountMono, memberCountMono,
						Mono.just(TimeUtils.getMillisUntil(start)))
						.flatMap(tuple -> {
							final User owner = tuple.getT1();
							final Long guildCount = tuple.getT2();
							final Long voiceChannelCount = tuple.getT3();
							final Long memberCount = tuple.getT4();
							final Long ping = tuple.getT5();

							return message.edit(spec -> spec.setContent("```prolog"
									+ String.format("%n-= Versions =-")
									+ String.format("%nJava: %s", System.getProperty("java.version"))
									+ String.format("%nShadbot: %s", Config.VERSION)
									+ String.format("%n%s: %s", D4J_NAME, D4J_VERSION)
									+ String.format("%nLavaPlayer: %s", PlayerLibrary.VERSION)
									+ String.format("%n%n-= Performance =-")
									+ String.format("%nMemory: %s/%s MB", FormatUtils.number(usedMemory), FormatUtils.number(maxMemory))
									+ String.format("%nCPU Usage: %.1f%%", Utils.getProcessCpuLoad())
									+ String.format("%nThreads: %s", FormatUtils.number(Thread.activeCount()))
									+ String.format("%n%n-= Internet =-")
									+ String.format("%nPing: %dms", ping)
									+ String.format("%nGateway Latency: %dms", context.getClient().getResponseTime())
									+ String.format("%n%n-= Shadbot =-")
									+ String.format("%nUptime: %s", DurationFormatUtils.formatDuration(uptime, "d 'day(s),' HH 'hour(s) and' mm 'minute(s)'", true))
									+ String.format("%nDeveloper: %s#%s", owner.getUsername(), owner.getDiscriminator())
									+ String.format("%nShard: %d/%d", context.getShardIndex() + 1, context.getShardCount())
									+ String.format("%nServers: %s", FormatUtils.number(guildCount))
									+ String.format("%nVoice Channels: %s", FormatUtils.number(voiceChannelCount))
									+ String.format("%nUsers: %s", FormatUtils.number(memberCount))
									+ "```"));
						}))
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show Shadbot's info.")
				.build();
	}
}
