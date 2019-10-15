package com.shadorc.shadbot.command.info;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import com.shadorc.shadbot.Config;
import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.TimeUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.common.GitProperties;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import org.apache.commons.lang3.time.DurationFormatUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class InfoCmd extends BaseCmd {

    private static final String D4J_NAME = GitProperties.getProperties().getProperty(GitProperties.APPLICATION_NAME);
    private static final String D4J_VERSION = GitProperties.getProperties().getProperty(GitProperties.APPLICATION_VERSION);
    private static final int MB_UNIT = 1024 * 1024;

    public InfoCmd() {
        super(CommandCategory.INFO, List.of("info"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final long uptime = TimeUtils.getMillisUntil(Shadbot.getLaunchTime());

        final Runtime runtime = Runtime.getRuntime();
        final long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / MB_UNIT;
        final long maxMemory = runtime.maxMemory() / MB_UNIT;

        return Mono.zip(context.getClient().getUserById(Shadbot.getOwnerId()),
                context.getClient().getGuilds().count(),
                context.getClient().getUsers().distinct().count(),
                context.getChannel())
                .flatMap(tuple -> {
                    final User owner = tuple.getT1();
                    final Long guildCount = tuple.getT2();
                    final Long memberCount = tuple.getT3();
                    final MessageChannel channel = tuple.getT4();

                    final long start = System.currentTimeMillis();
                    return DiscordUtils.sendMessage(String.format(Emoji.GEAR + " (**%s**) Testing ping...", context.getUsername()), channel)
                            .flatMap(message -> message.edit(spec -> spec.setContent("```prolog"
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
                                    + String.format("%nPing: %dms", TimeUtils.getMillisUntil(start))
                                    + String.format("%nGateway Latency: %dms", context.getClient().getResponseTime())
                                    + String.format("%n%n-= Shadbot =-")
                                    + String.format("%nUptime: %s", DurationFormatUtils.formatDuration(uptime, "d 'day(s),' HH 'hour(s) and' mm 'minute(s)'", true))
                                    + String.format("%nDeveloper: %s#%s", owner.getUsername(), owner.getDiscriminator())
                                    + String.format("%nShard: %d/%d", context.getShardIndex() + 1, context.getShardCount())
                                    + String.format("%nServers: %s", FormatUtils.number(guildCount))
                                    + String.format("%nVoice Channels: %d (GM: %d)", MusicManager.getInstance().getGuildIdsWithVoice().size(), MusicManager.getInstance().getGuildIdsWithGuildMusics().size())
                                    + String.format("%nUnique Users: %s", FormatUtils.number(memberCount))
                                    + "```")));
                })
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show Shadbot's info.")
                .build();
    }
}
