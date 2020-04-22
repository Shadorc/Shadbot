package com.shadorc.shadbot.command.info;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.TimeUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.common.GitProperties;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.core.spec.EmbedCreateSpec;
import org.apache.commons.lang3.time.DurationFormatUtils;
import reactor.core.publisher.Mono;

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
        return Mono.zip(context.getClient().getUserById(Shadbot.getOwnerId()),
                context.getClient().withRetrievalStrategy(EntityRetrievalStrategy.STORE).getGuilds().count(),
                context.getChannel())
                .flatMap(tuple -> {
                    final User owner = tuple.getT1();
                    final long guildCount = tuple.getT2();
                    final MessageChannel channel = tuple.getT3();

                    final long start = System.currentTimeMillis();
                    return DiscordUtils.sendMessage(String.format(Emoji.GEAR + " (**%s**) Testing ping...",
                            context.getUsername()), channel)
                            .flatMap(message -> message.edit(spec -> spec.setContent("```prolog"
                                    + this.getVersionSection()
                                    + this.getPerformanceSection()
                                    + this.getInternetSection(context, start)
                                    + this.getShadbotSection(context, owner, guildCount)
                                    + "```")));
                })
                .then();
    }

    private String getVersionSection() {
        return String.format("%n-= Versions =-")
                + String.format("%nJava: %s", JAVA_VERSION)
                + String.format("%nShadbot: %s", Config.VERSION)
                + String.format("%n%s: %s", D4J_NAME, D4J_VERSION)
                + String.format("%nLavaPlayer: %s", LAVAPLAYER_VERSION);
    }

    private String getPerformanceSection() {
        return String.format("%n%n-= Performance =-")
                + String.format("%nMemory: %s/%s MB", FormatUtils.number(Utils.getMemoryUsed()), FormatUtils.number(Utils.getMaxMemory()))
                + String.format("%nCPU (Process): %.1f%%", Utils.getCpuUsage())
                + String.format("%nThreads: %s", FormatUtils.number(Thread.activeCount()));
    }

    private String getInternetSection(Context context, long start) {
        final long gatewayLatency = context.getClient().getGatewayClientGroup()
                .find(context.getEvent().getShardInfo().getIndex())
                .orElseThrow()
                .getResponseTime()
                .toMillis();

        return String.format("%n%n-= Internet =-")
                + String.format("%nPing: %dms", TimeUtils.getMillisUntil(start))
                + String.format("%nGateway Latency: %dms", gatewayLatency);
    }

    private String getShadbotSection(Context context, User owner, long guildCount) {
        final String uptime = DurationFormatUtils.formatDuration(TimeUtils.getMillisUntil(Shadbot.getLaunchTime()),
                "d 'day(s),' HH 'hour(s) and' mm 'minute(s)'", true);
        final long voiceChannelCount = context.getClient()
                .getGuilds()
                .flatMap(Guild::getVoiceStates)
                .flatMap(VoiceState::getUser)
                .filter(user -> user.getId().equals(Shadbot.getSelfId()))
                .count()
                .block();
        final long guildManagerCount = MusicManager.getInstance().getGuildMusicCount();

        return String.format("%n%n-= Shadbot =-")
                + String.format("%nUptime: %s", uptime)
                + String.format("%nDeveloper: %s#%s", owner.getUsername(), owner.getDiscriminator())
                + String.format("%nShard: %d/%d", context.getShardIndex() + 1, context.getShardCount())
                + String.format("%nServers: %s", FormatUtils.number(guildCount))
                + String.format("%nVoice Channels: %d (GM: %d)", voiceChannelCount, guildManagerCount);
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Show Shadbot's info.")
                .build();
    }
}
