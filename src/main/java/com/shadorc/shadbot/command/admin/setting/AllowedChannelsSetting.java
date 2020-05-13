package com.shadorc.shadbot.command.admin.setting;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.setting.BaseSetting;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Snowflake;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AllowedChannelsSetting extends BaseSetting {

    private enum Action {
        ADD, REMOVE;
    }

    public AllowedChannelsSetting() {
        super(List.of("allowed_channels", "allowed-channels", "allowed_channel", "allowed-channel"),
                Setting.ALLOWED_CHANNELS, "Manage in which channel(s) Shadbot can talk in.");
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(3);

        final Action action = Utils.parseEnum(Action.class, args.get(1),
                new CommandException(String.format("`%s` is not a valid action. %s",
                        args.get(1), FormatUtils.options(Action.class))));

        return context.getGuild()
                .flatMapMany(guild -> DiscordUtils.extractChannels(guild, args.get(2)))
                .collectList()
                .flatMap(mentionedChannels -> {
                    if (mentionedChannels.isEmpty()) {
                        return Mono.error(new CommandException(String.format("Channel `%s` not found.", args.get(2))));
                    }

                    return Mono.zip(Mono.just(mentionedChannels),
                            DatabaseManager.getGuilds().getDBGuild(context.getGuildId()));
                })
                .flatMap(tuple -> {
                    final List<GuildChannel> mentionedChannels = tuple.getT1();
                    final DBGuild dbGuild = tuple.getT2();

                    final Set<Snowflake> allowedTextChannelIds = dbGuild.getSettings().getAllowedTextChannelIds();
                    final Set<Snowflake> allowedVoiceChannelIds = dbGuild.getSettings().getAllowedVoiceChannelIds();

                    final Set<Snowflake> mentionedVoiceChannelIds = mentionedChannels.stream()
                            .filter(channel -> channel.getType() == Channel.Type.GUILD_VOICE)
                            .map(Channel::getId)
                            .collect(Collectors.toUnmodifiableSet());

                    final Set<Snowflake> mentionedTextChannelIds = mentionedChannels.stream()
                            .filter(channel -> channel.getType() == Channel.Type.GUILD_TEXT)
                            .map(Channel::getId)
                            .collect(Collectors.toUnmodifiableSet());

                    final StringBuilder strBuilder = new StringBuilder();
                    if (action == Action.ADD) {
                        if (allowedTextChannelIds.isEmpty() && !mentionedTextChannelIds.isEmpty()
                                && mentionedTextChannelIds.stream().noneMatch(channelId -> channelId.equals(context.getChannelId()))) {
                            strBuilder.append(Emoji.WARNING + " You did not mentioned this channel. "
                                    + "I will not reply here until this channel is added to the list of allowed channels.\n");
                        }

                        for (final Channel channel : mentionedChannels) {
                            final Snowflake channelId = channel.getId();
                            if (channel.getType() == Channel.Type.GUILD_TEXT && !allowedTextChannelIds.contains(channelId)) {
                                allowedTextChannelIds.add(channelId);
                            } else if (channel.getType() == Channel.Type.GUILD_VOICE && !allowedVoiceChannelIds.contains(channelId)) {
                                allowedVoiceChannelIds.add(channelId);
                            }
                        }

                        strBuilder.append(String.format(Emoji.CHECK_MARK + " %s added to allowed channels.",
                                FormatUtils.format(mentionedChannels, Channel::getMention, ", ")));

                    } else {
                        allowedTextChannelIds.removeAll(mentionedTextChannelIds);
                        allowedVoiceChannelIds.removeAll(mentionedVoiceChannelIds);
                        strBuilder.append(String.format(Emoji.CHECK_MARK + " %s removed from allowed channels.",
                                FormatUtils.format(mentionedChannels, Channel::getMention, ", ")));

                        if (!mentionedTextChannelIds.isEmpty() && allowedTextChannelIds.isEmpty()) {
                            strBuilder.append("\n" + Emoji.INFO + " There are no more allowed text channels set, "
                                    + "I can now speak in all the text channels.");
                        }
                        if (!mentionedVoiceChannelIds.isEmpty() && allowedVoiceChannelIds.isEmpty()) {
                            strBuilder.append("\n" + Emoji.INFO + " There are no more allowed voice channels set, "
                                    + "I can now connect to all voice channels.");
                        }
                    }

                    return dbGuild.setSetting(Setting.ALLOWED_TEXT_CHANNELS, allowedTextChannelIds)
                            .then(dbGuild.setSetting(Setting.ALLOWED_VOICE_CHANNELS, allowedVoiceChannelIds))
                            .thenReturn(strBuilder);
                })
                .map(StringBuilder::toString)
                .flatMap(text -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(text, channel)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return DiscordUtils.getDefaultEmbed()
                .andThen(embed -> embed.addField("Usage", String.format("`%s%s <action> <#channel(s)>`",
                        context.getPrefix(), this.getCommandName()), false)
                        .addField("Argument", String.format("**action** - %s%n**channel(s)** - the (voice) channel(s) to %s",
                                FormatUtils.format(Action.class, "/"),
                                FormatUtils.format(Action.class, "/")), false)
                        .addField("Example", String.format("`%s%s add #general`",
                                context.getPrefix(), this.getCommandName()), false));
    }

}
