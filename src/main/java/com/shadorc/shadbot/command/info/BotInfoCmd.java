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
import java.util.Locale;
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
                    return context.reply(Emoji.GEAR, context.localize("testing.ping"))
                            .then(context.editReply(BotInfoCmd.formatEmbed(context, start, owner, guildCount)));
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
        final Locale locale = context.getLocale();

        final String shadbotTitle = "%s Shadbot".formatted(Emoji.ROBOT);
        final String shadbotField = context.localize("botinfo.field.shadbot")
                .formatted(uptime, owner.getTag(),
                        shardInfo.getIndex() + 1, shardInfo.getCount(),
                        FormatUtil.number(guildCount, locale),
                        FormatUtil.number(Telemetry.VOICE_COUNT_GAUGE.get(), locale));

        final String networkTitle = "%s %s".formatted(Emoji.SATELLITE, context.localize("botinfo.title.network"));
        final String networkField = context.localize("botinfo.field.network")
                .formatted(TimeUtil.elapsed(start), gatewayLatency);

        final String versionsTitle = "%s %s".formatted(Emoji.SCREWDRIVER, context.localize("botinfo.title.versions"));
        final String versionsField = context.localize("botinfo.field.versions")
                .formatted(JAVA_VERSION, Config.VERSION, D4J_NAME, D4J_VERSION, LAVAPLAYER_VERSION);

        final String performanceTitle = "%s %s".formatted(Emoji.GEAR, context.localize("botinfo.title.performance"));
        final String performanceField = context.localize("botinfo.field.performance")
                .formatted(FormatUtil.number(SystemUtil.getUsedHeapMemory(), locale),
                        FormatUtil.number(SystemUtil.getMaxHeapMemory(), locale),
                        SystemUtil.getProcessCpuUsage(),
                        FormatUtil.number(SystemUtil.getThreadCount(), locale));

        return ShadbotUtil.getDefaultEmbed(embed -> embed
                .setAuthor(context.localize("botinfo.title"), null, context.getAuthorAvatar())
                .addField(shadbotTitle, shadbotField, true)
                .addField(networkTitle, networkField, true)
                .addField(versionsTitle, versionsField, true)
                .addField(performanceTitle, performanceField, true));
    }

}
