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
                                    InfoCmd.formatContent(context, start, owner, guildCount)));
                }));
    }

    private static String formatContent(Context context, long start, User owner, long guildCount) {
        final long gatewayLatency = context.getClient().getGatewayClientGroup()
                .find(context.getEvent().getShardInfo().getIndex())
                .map(GatewayClient::getResponseTime)
                .map(Duration::toMillis)
                .orElseThrow();
        final String uptime = FormatUtil.formatDurationWords(
                Duration.ofMillis(TimeUtil.getMillisUntil(Shadbot.getLaunchTime())));

        return """
                ```prolog
                -= Versions =-
                Java: %s
                Shadbot: %s
                %s: %s
                LavaPlayer: %s
                                
                -= Performance =-
                Memory: %s/%s MB
                CPU (Process): %.1f%%
                Threads: %s
                                
                -= Internet =-
                Ping: %dms
                Gateway Latency: %dms
                                
                -= Shadbot =-
                Uptime: %s
                Developer: %s
                Shard: %d/%d
                Servers: %s
                Voice Channels: %s
                ```
                """
                .formatted(JAVA_VERSION, Config.VERSION, D4J_NAME, D4J_VERSION, LAVAPLAYER_VERSION,
                        FormatUtil.number(ProcessUtil.getMemoryUsed()), FormatUtil.number(ProcessUtil.getMaxMemory()),
                        ProcessUtil.getCpuUsage(), FormatUtil.number(Thread.activeCount()),
                        TimeUtil.getMillisUntil(start), gatewayLatency,
                        uptime, owner.getTag(), context.getShardIndex() + 1, context.getShardCount(),
                        FormatUtil.number(guildCount), FormatUtil.number(Telemetry.VOICE_COUNT_GAUGE.get()));
    }

}
