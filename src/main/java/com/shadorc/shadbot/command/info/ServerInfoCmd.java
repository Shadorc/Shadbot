package com.shadorc.shadbot.command.info;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
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
import java.time.format.FormatStyle;
import java.util.List;
import java.util.function.Consumer;

public class ServerInfoCmd extends BaseCmd {

    private final DateTimeFormatter dateFormatter;

    public ServerInfoCmd() {
        super(CommandCategory.INFO, "server", "Show server info");
        this.dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.MEDIUM);
    }

    @Override
    public Mono<?> execute(Context context) {
        final Mono<Guild> getGuild = context.getGuild().cache();
        return Mono.zip(Mono.just(context),
                getGuild,
                getGuild.flatMapMany(Guild::getChannels).collectList(),
                getGuild.flatMap(Guild::getOwner),
                getGuild.flatMap(Guild::getRegion))
                .map(TupleUtils.function(this::formatEmbed))
                .flatMap(context::createFollowupMessage);
    }

    private Consumer<EmbedCreateSpec> formatEmbed(Context context, Guild guild, List<GuildChannel> channels,
                                                  Member owner, Region region) {
        final LocalDateTime creationTime = TimeUtil.toLocalDateTime(guild.getId().getTimestamp());
        final long voiceChannels = channels.stream().filter(VoiceChannel.class::isInstance).count();
        final long textChannels = channels.stream().filter(TextChannel.class::isInstance).count();

        final DateTimeFormatter dateFormatter = this.dateFormatter.withLocale(context.getLocale());

        final String idTitle = Emoji.ID + " " + context.localize("serverinfo.id");
        final String ownerTitle = Emoji.CROWN + " " + context.localize("serverinfo.owner");
        final String regionTitle = Emoji.MAP + " " + context.localize("serverinfo.region");
        final String creationTitle = Emoji.BIRTHDAY + " " + context.localize("serverinfo.creation");
        final String creationField = "%s\n(%s)"
                .formatted(creationTime.format(dateFormatter),
                        FormatUtil.formatLongDuration(context.getLocale(), creationTime));
        final String channelsTitle = Emoji.SPEECH_BALLOON + " " + context.localize("serverinfo.channels");
        final String channelsField = context.localize("serverinfo.channels.field")
                .formatted(Emoji.MICROPHONE, voiceChannels, Emoji.KEYBOARD, textChannels);
        final String membersTitle = Emoji.BUSTS_IN_SILHOUETTE + " " + context.localize("serverinfo.members");

        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor(context.localize("serverinfo.title").formatted(guild.getName()), null,
                        context.getAuthorAvatar())
                        .setThumbnail(guild.getIconUrl(Format.JPEG).orElse(""))
                        .addField(idTitle, guild.getId().asString(), true)
                        .addField(ownerTitle, owner.getTag(), true)
                        .addField(regionTitle, region.getName(), true)
                        .addField(creationTitle, creationField, true)
                        .addField(channelsTitle, channelsField, true)
                        .addField(membersTitle, context.localize(guild.getMemberCount()), true));
    }

}
