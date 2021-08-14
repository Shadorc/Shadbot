package com.shadorc.shadbot.command.info;

import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.command.GroupCmd;
import com.shadorc.shadbot.core.command.SubCmd;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.common.util.TimestampFormat;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Image.Format;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.Instant;
import java.util.List;

public class ServerInfoCmd extends SubCmd {

    public ServerInfoCmd(final GroupCmd groupCmd) {
        super(groupCmd, CommandCategory.INFO, "server", "Show server info");
    }

    @Override
    public Mono<?> execute(Context context) {
        final Mono<Guild> getGuild = context.getGuild().cache();
        return Mono.zip(Mono.just(context),
                        getGuild,
                        getGuild.flatMapMany(Guild::getChannels).collectList(),
                        getGuild.flatMap(Guild::getOwner))
                .map(TupleUtils.function(this::formatEmbed))
                .flatMap(context::createFollowupMessage);
    }

    private EmbedCreateSpec formatEmbed(Context context, Guild guild, List<GuildChannel> channels, Member owner) {
        final long voiceChannels = channels.stream().filter(VoiceChannel.class::isInstance).count();
        final long textChannels = channels.stream().filter(TextChannel.class::isInstance).count();

        final String idTitle = Emoji.ID + " " + context.localize("serverinfo.id");
        final String ownerTitle = Emoji.CROWN + " " + context.localize("serverinfo.owner");
        final String creationTitle = Emoji.BIRTHDAY + " " + context.localize("serverinfo.creation");
        final Instant creationInstant = guild.getId().getTimestamp();
        final String creationField = "%s\n(%s)"
                .formatted(TimestampFormat.SHORT_DATE_TIME.format(creationInstant),
                        FormatUtil.formatRelativeTime(context.getLocale(), creationInstant));
        final String channelsTitle = Emoji.SPEECH_BALLOON + " " + context.localize("serverinfo.channels");
        final String channelsField = context.localize("serverinfo.channels.field")
                .formatted(Emoji.MICROPHONE, voiceChannels, Emoji.KEYBOARD, textChannels);
        final String membersTitle = Emoji.BUSTS_IN_SILHOUETTE + " " + context.localize("serverinfo.members");

        return ShadbotUtil.createEmbedBuilder()
                .author(context.localize("serverinfo.title").formatted(guild.getName()), null,
                        context.getAuthorAvatar())
                .thumbnail(guild.getIconUrl(Format.JPEG).orElse(""))
                .addField(idTitle, guild.getId().asString(), true)
                .addField(ownerTitle, owner.getTag(), true)
                .addField(creationTitle, creationField, true)
                .addField(channelsTitle, channelsField, true)
                .addField(membersTitle, context.localize(guild.getMemberCount()), true)
                .build();
    }

}
