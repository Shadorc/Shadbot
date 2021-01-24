package com.shadorc.shadbot.command.info;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import com.shadorc.shadbot.utils.TimeUtil;
import discord4j.core.object.Region;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Image.Format;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ServerInfoCmd extends BaseCmd {

    private final DateTimeFormatter dateFormatter;

    public ServerInfoCmd() {
        super(CommandCategory.INFO, "server_info", "Show info about this server");
        this.setDefaultRateLimiter();

        this.dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy - HH'h'mm", Locale.ENGLISH);
    }

    @Override
    public Mono<?> execute(Context context) {
        final Mono<Guild> getGuild = context.getGuild().cache();
        return Mono.zip(getGuild,
                getGuild.flatMapMany(Guild::getChannels).collectList(),
                getGuild.flatMap(Guild::getOwner),
                getGuild.flatMap(Guild::getRegion),
                Mono.just(context.getAuthorAvatarUrl()))
                .map(TupleUtils.function(this::getEmbed))
                .flatMap(context::createFollowupMessage);
    }

    private Consumer<EmbedCreateSpec> getEmbed(Guild guild, List<GuildChannel> channels, Member owner, Region region, String avatarUrl) {
        final LocalDateTime creationTime = TimeUtil.toLocalDateTime(guild.getId().getTimestamp());
        final String creationDate = String.format("%s%n(%s)",
                creationTime.format(this.dateFormatter), FormatUtil.formatLongDuration(creationTime));
        final long voiceChannels = channels.stream().filter(VoiceChannel.class::isInstance).count();
        final long textChannels = channels.stream().filter(TextChannel.class::isInstance).count();

        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor(String.format("Server Info: %s", guild.getName()), null, avatarUrl)
                        .setThumbnail(guild.getIconUrl(Format.JPEG).orElse(""))
                        .addField("Server ID", guild.getId().asString(), false)
                        .addField("Owner", owner.getUsername(), false)
                        .addField("Region", region.getName(), false)
                        .addField("Creation date", creationDate, false)
                        .addField("Channels", String.format("**Voice:** %d%n**Text:** %d", voiceChannels, textChannels), false)
                        .addField("Members", FormatUtil.number(guild.getMemberCount()), false));
    }

}
