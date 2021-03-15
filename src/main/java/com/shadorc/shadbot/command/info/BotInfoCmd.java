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
import com.shadorc.shadbot.utils.ShadbotUtil;
import com.shadorc.shadbot.utils.SystemUtil;
import com.shadorc.shadbot.utils.TimeUtil;
import discord4j.common.GitProperties;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.gateway.GatewayClient;
import discord4j.gateway.ShardInfo;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.Duration;
import java.util.Properties;
import java.util.function.Consumer;

class BotInfoCmd extends BaseCmd {

    private static final String JAVA_VERSION = System.getProperty("java.version");
    private static final Properties D4J_PROPERTIES = GitProperties.getProperties();
    private static final String D4J_NAME = D4J_PROPERTIES.getProperty(GitProperties.APPLICATION_NAME);
    private static final String D4J_VERSION = D4J_PROPERTIES.getProperty(GitProperties.APPLICATION_VERSION);
    private static final String LAVAPLAYER_VERSION = PlayerLibrary.VERSION;

    public BotInfoCmd() {
        super(CommandCategory.INFO, "bot", "Show bot info");
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
                            .flatMap(messageId -> context.editReply(messageId,
                                    BotInfoCmd.formatEmbed(context, start, owner, guildCount)));
                }));
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(Context context, long start, User owner, long guildCount) {
        final long gatewayLatency = context.getClient().getGatewayClientGroup()
                .find(context.getEvent().getShardInfo().getIndex())
                .map(GatewayClient::getResponseTime)
                .map(Duration::toMillis)
                .orElseThrow();
        final String uptime = FormatUtil.formatDurationWords(Duration.ofMillis(SystemUtil.getUptime()));
        final ShardInfo shardInfo = context.getEvent().getShardInfo();

        return ShadbotUtil.getDefaultEmbed(embed -> embed
                .setAuthor("Bot Info", null, context.getAuthorAvatar())
                .addField(Emoji.ROBOT + " Shadbot", "**Uptime:** %s%n**Developer:** %s%n**Shard:** %d/%d%n**Servers:** %s%n**Voice Channels:** %s"
                        .formatted(uptime, owner.getTag(), shardInfo.getIndex() + 1, shardInfo.getCount(),
                                FormatUtil.number(guildCount), FormatUtil.number(Telemetry.VOICE_COUNT_GAUGE.get())), true)
                .addField(Emoji.SATELLITE + " Network", "**Ping:** %dms%n**Gateway:** %dms"
                        .formatted(TimeUtil.elapsed(start), gatewayLatency), true)
                .addField(Emoji.SCREWDRIVER + " Versions", "**Java:** %s%n**Shadbot:** %s%n**%s:** %s%n**LavaPlayer:** %s"
                        .formatted(JAVA_VERSION, Config.VERSION, D4J_NAME, D4J_VERSION, LAVAPLAYER_VERSION), true)
                .addField(Emoji.GEAR + " Performance", "**Memory:** %s/%s MB%n**CPU (Process):** %.1f%%%n**Threads:** %s"
                        .formatted(FormatUtil.number(SystemUtil.getUsedHeapMemory()), FormatUtil.number(SystemUtil.getMaxHeapMemory()),
                                SystemUtil.getProcessCpuUsage(), FormatUtil.number(SystemUtil.getThreadCount())), true));
    }

}
