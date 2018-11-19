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
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
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
		final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		final long uptime = TimeUtils.getMillisUntil(Shadbot.getLaunchTime());

		final Runtime runtime = Runtime.getRuntime();
		final long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / MB_UNIT;
		final long maxMemory = runtime.maxMemory() / MB_UNIT;

		final Mono<Long> voiceChannelsCountMono = Flux.fromIterable(Shadbot.getClients())
				.flatMap(DiscordClient::getGuilds)
				.flatMap(guild -> guild.getMemberById(context.getSelfId()))
				.flatMap(Member::getVoiceState)
				.flatMap(VoiceState::getChannel)
				.count();
		
		final Mono<Long> guildsCountMono = Flux.fromIterable(Shadbot.getClients())
				.flatMap(DiscordClient::getGuilds)
				.count();

		final Mono<Long> membersCountMono = Flux.fromIterable(Shadbot.getClients())
				.flatMap(DiscordClient::getUsers)
				.count();

		return Mono.zip(context.getClient().getApplicationInfo().flatMap(ApplicationInfo::getOwner),
				guildsCountMono, voiceChannelsCountMono, membersCountMono)
				.map(tuple -> {
					final User owner = tuple.getT1();
					final Long guildsCount = tuple.getT2();
					final Long voiceChannelsCount = tuple.getT3();
					final Long membersCount = tuple.getT4();

					return "```prolog"
							+ String.format("%n-= Performance Info =-")
							+ String.format("%nMemory: %s/%s MB", FormatUtils.number(usedMemory), FormatUtils.number(maxMemory))
							+ String.format("%nCPU Usage: %.1f%%", Utils.getProcessCpuLoad())
							+ String.format("%nThreads Count: %s", FormatUtils.number(Thread.activeCount()))
							+ String.format("%n%n-= APIs Info =-")
							+ String.format("%nJava Version: %s", System.getProperty("java.version"))
							+ String.format("%n%s Version: %s", D4J_NAME, D4J_VERSION)
							+ String.format("%nLavaPlayer Version: %s", PlayerLibrary.VERSION)
							+ String.format("%n%n-= Shadbot Info =-")
							+ String.format("%nUptime: %s", DurationFormatUtils.formatDuration(uptime, "d 'day(s),' HH 'hour(s) and' mm 'minute(s)'", true))
							+ String.format("%nDeveloper: %s#%s", owner.getUsername(), owner.getDiscriminator())
							+ String.format("%nShadbot Version: %s", Config.VERSION)
							+ String.format("%nShard: %d/%d", context.getShardIndex() + 1, context.getShardCount())
							+ String.format("%nServers: %s", FormatUtils.number(guildsCount))
							+ String.format("%nVoice Channels: %s", FormatUtils.number(voiceChannelsCount))
							+ String.format("%nUsers: %s", FormatUtils.number(membersCount))
							+ "```";
				})
				.flatMap(loadingMsg::send)
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show Shadbot's info.")
				.build();
	}
}
