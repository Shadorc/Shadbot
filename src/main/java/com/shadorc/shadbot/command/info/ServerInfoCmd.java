package com.shadorc.shadbot.command.info;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.object.Emoji;
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
import java.util.function.Consumer;

class ServerInfoCmd extends BaseCmd {

    private final DateTimeFormatter dateFormatter;

    public ServerInfoCmd() {
        super(CommandCategory.INFO, "server", "Show server info");
        this.dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy - HH'h'mm", Config.DEFAULT_LOCALE);
    }

    @Override
    public Mono<?> execute(Context context) {
        final Mono<Guild> getGuild = context.getGuild().cache();
        return Mono.zip(getGuild,
                getGuild.flatMapMany(Guild::getChannels).collectList(),
                getGuild.flatMap(Guild::getOwner),
                getGuild.flatMap(Guild::getRegion),
                Mono.just(context.getAuthorAvatar()))
                .map(TupleUtils.function(this::formatEmbed))
                .flatMap(context::createFollowupMessage);
    }

    private Consumer<EmbedCreateSpec> formatEmbed(Guild guild, List<GuildChannel> channels, Member owner, Region region, String avatarUrl) {
        final LocalDateTime creationTime = TimeUtil.toLocalDateTime(guild.getId().getTimestamp());
        final String creationDate = String.format("%s%n(%s)",
                creationTime.format(this.dateFormatter), FormatUtil.formatLongDuration(creationTime));
        final long voiceChannels = channels.stream().filter(VoiceChannel.class::isInstance).count();
        final long textChannels = channels.stream().filter(TextChannel.class::isInstance).count();

        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor(String.format("Server Info: %s", guild.getName()), null, avatarUrl)
                        .setThumbnail(guild.getIconUrl(Format.JPEG).orElse(""))
                        .addField(Emoji.ID + " Server ID", guild.getId().asString(), true)
                        .addField(Emoji.CROWN + " Owner", owner.getTag(), true)
                        .addField(Emoji.MAP + " Region", region.getName(), true)
                        .addField(Emoji.BIRTHDAY + " Creation date", creationDate, true)
                        .addField(Emoji.SPEECH_BALLOON + " Channels", String.format("%s **Voice:** %d%n%s **Text:** %d",
                                Emoji.MICROPHONE, voiceChannels, Emoji.KEYBOARD, textChannels), true)
                        .addField(Emoji.BUSTS_IN_SILHOUETTE + " Members", FormatUtil.number(guild.getMemberCount()), true));
    }

}
