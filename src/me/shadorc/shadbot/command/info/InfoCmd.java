package me.shadorc.shadbot.command.info;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import discord4j.common.GitProperties;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.music.MusicManager;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.object.message.LoadingMessage;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
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

        final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

        return Mono.zip(context.getClient().getUserById(Snowflake.of(Shadbot.OWNER_ID.get())),
                context.getClient().getGuilds().count(),
                context.getClient().getUsers().count())
                .flatMap(tuple -> {
                    final User owner = tuple.getT1();
                    final Long guildCount = tuple.getT2();
                    final Long memberCount = tuple.getT3();

                    final long start = System.currentTimeMillis();
                    return loadingMsg.setContent(String.format(Emoji.GEAR + " (**%s**) Testing ping...", context.getUsername()))
                            .send()
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
                                    + String.format("%nVoice Channels: %d (GM: %d)", MusicManager.getGuildIdsWithVoice().size(), MusicManager.getGuildIdsWithGuildMusics().size())
                                    + String.format("%nUsers: %s", FormatUtils.number(memberCount))
                                    + "```")));
                })
                .doOnTerminate(loadingMsg::stopTyping)
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show Shadbot's info.")
                .build();
    }
}
