package com.shadorc.shadbot.command.info;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ProcessUtil;
import com.shadorc.shadbot.utils.TimeUtil;
import discord4j.common.GitProperties;
import discord4j.core.object.entity.User;
import discord4j.gateway.GatewayClient;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.Duration;
import java.util.Properties;

public class InfoCmd extends BaseCmd {

    private static final String JAVA_VERSION = System.getProperty("java.version");
    private static final Properties D4J_PROPERTIES = GitProperties.getProperties();
    private static final String D4J_NAME = D4J_PROPERTIES.getProperty(GitProperties.APPLICATION_NAME);
    private static final String D4J_VERSION = D4J_PROPERTIES.getProperty(GitProperties.APPLICATION_VERSION);
    private static final String LAVAPLAYER_VERSION = PlayerLibrary.VERSION;

    public InfoCmd() {
        super(CommandCategory.INFO, "info", "Show info");
    }

    @Override
    public Mono<?> execute(Context context) {
        return Mono.zip(
                context.getClient().getUserById(Shadbot.getOwnerId()),
                context.getChannel(),
                context.getClient().getGuilds().count())
                .flatMap(TupleUtils.function((owner, channel, guildCount) -> {
                    final long start = System.currentTimeMillis();
                    return context.createFollowupMessage(Emoji.GEAR + " (**%s**) Testing ping...", context.getAuthorName())
                            .flatMap(messageId -> context.editFollowupMessage(messageId,
                                    "```prolog%s%s%s%s```",
                                    InfoCmd.getVersionSection(),
                                    InfoCmd.getPerformanceSection(),
                                    InfoCmd.getInternetSection(context, start),
                                    InfoCmd.getShadbotSection(context, owner, guildCount)));
                }));
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
                FormatUtil.number(ProcessUtil.getMemoryUsed()), FormatUtil.number(ProcessUtil.getMaxMemory()))
                + String.format("%nCPU (Process): %.1f%%", ProcessUtil.getCpuUsage())
                + String.format("%nThreads: %s", FormatUtil.number(Thread.activeCount()));
    }

    private static String getInternetSection(Context context, long start) {
        final long gatewayLatency = context.getClient().getGatewayClientGroup()
                .find(context.getEvent().getShardInfo().getIndex())
                .map(GatewayClient::getResponseTime)
                .map(Duration::toMillis)
                .orElseThrow();

        return String.format("%n%n-= Internet =-")
                + String.format("%nPing: %dms", TimeUtil.getMillisUntil(start))
                + String.format("%nGateway Latency: %dms", gatewayLatency);
    }

    private static String getShadbotSection(Context context, User owner, long guildCount) {
        final String uptime = FormatUtil.formatDurationWords(
                Duration.ofMillis(TimeUtil.getMillisUntil(Shadbot.getLaunchTime())));

        return String.format("%n%n-= Shadbot =-")
                + String.format("%nUptime: %s", uptime)
                + String.format("%nDeveloper: %s", owner.getTag())
                + String.format("%nShard: %d/%d", context.getShardIndex() + 1, context.getShardCount())
                + String.format("%nServers: %s", FormatUtil.number(guildCount))
                + String.format("%nVoice Channels: %s", FormatUtil.number(Telemetry.VOICE_COUNT_GAUGE.get()));
    }

}
