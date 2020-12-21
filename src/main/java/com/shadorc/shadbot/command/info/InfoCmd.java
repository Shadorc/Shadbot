package com.shadorc.shadbot.command.info;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.cache.CacheManager;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.ProcessUtils;
import com.shadorc.shadbot.utils.TimeUtils;
import discord4j.common.GitProperties;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.gateway.GatewayClient;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

public class InfoCmd extends BaseCmd {

    private static final String JAVA_VERSION = System.getProperty("java.version");
    private static final Properties D4J_PROPERTIES = GitProperties.getProperties();
    private static final String D4J_NAME = D4J_PROPERTIES.getProperty(GitProperties.APPLICATION_NAME);
    private static final String D4J_VERSION = D4J_PROPERTIES.getProperty(GitProperties.APPLICATION_VERSION);
    private static final String LAVAPLAYER_VERSION = PlayerLibrary.VERSION;

    public InfoCmd() {
        super(CommandCategory.INFO, List.of("info"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        return Mono.zip(
                context.getClient().getUserById(Shadbot.getOwnerId()),
                context.getChannel())
                .flatMap(TupleUtils.function((owner, channel) -> {
                    final long start = System.currentTimeMillis();
                    return DiscordUtils.sendMessage(String.format(Emoji.GEAR + " (**%s**) Testing ping...",
                            context.getUsername()), channel)
                            .flatMap(message -> message.edit(spec -> spec.setContent("```prolog"
                                    + InfoCmd.getVersionSection()
                                    + InfoCmd.getPerformanceSection()
                                    + InfoCmd.getInternetSection(context, start)
                                    + InfoCmd.getShadbotSection(context, owner)
                                    + "```")));
                }))
                .then();
    }

    private static String getVersionSection() {
        return String.format("%n-= Versions =-")
                + String.format("%nJava: %s", JAVA_VERSION)
                + String.format("%nShadbot: %s", Config.VERSION)
                + String.format("%n%s: %s", D4J_NAME, D4J_VERSION)
                + String.format("%nLavaPlayer: %s", LAVAPLAYER_VERSION);
    }

    private static String getPerformanceSection() {
        return String.format("%n%n-= Performance =-")
                + String.format("%nMemory: %s/%s MB",
                FormatUtils.number(ProcessUtils.getMemoryUsed()), FormatUtils.number(ProcessUtils.getMaxMemory()))
                + String.format("%nCPU (Process): %.1f%%", ProcessUtils.getCpuUsage())
                + String.format("%nThreads: %s", FormatUtils.number(Thread.activeCount()));
    }

    private static String getInternetSection(Context context, long start) {
        final long gatewayLatency = context.getClient().getGatewayClientGroup()
                .find(context.getEvent().getShardInfo().getIndex())
                .map(GatewayClient::getResponseTime)
                .map(Duration::toMillis)
                .orElse(-1L);

        return String.format("%n%n-= Internet =-")
                + String.format("%nPing: %dms", TimeUtils.getMillisUntil(start))
                + String.format("%nGateway Latency: %dms", gatewayLatency);
    }

    private static String getShadbotSection(Context context, User owner) {
        final String uptime = FormatUtils.formatDurationWords(
                Duration.ofMillis(TimeUtils.getMillisUntil(Shadbot.getLaunchTime())));

        return String.format("%n%n-= Shadbot =-")
                + String.format("%nUptime: %s", uptime)
                + String.format("%nDeveloper: %s", owner.getTag())
                + String.format("%nShard: %d/%d", context.getShardIndex() + 1, context.getShardCount())
                + String.format("%nServers: %s", FormatUtils.number(CacheManager.getInstance().getGuildOwnersCache().count()))
                + String.format("%nVoice Channels: %s", FormatUtils.number(Telemetry.VOICE_COUNT_GAUGE.get()));
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Show Shadbot's info.")
                .build();
    }
}
