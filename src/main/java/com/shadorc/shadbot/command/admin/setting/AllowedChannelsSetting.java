package com.shadorc.shadbot.command.admin.setting;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.setting.BaseSetting;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AllowedChannelsSetting extends BaseSetting {

    private enum Action {
        ADD, REMOVE;
    }

    public AllowedChannelsSetting() {
        super(Setting.ALLOWED_CHANNELS, "Manage channels allowed to Shadbot.");
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(3);

        final Action action = Utils.parseEnum(Action.class, args.get(1),
                new CommandException(String.format("`%s` is not a valid action. %s",
                        args.get(1), FormatUtils.options(Action.class))));

        return context.getGuild()
                .flatMapMany(guild -> DiscordUtils.extractChannels(guild, args.get(2)))
                .flatMap(channelId -> context.getClient().getChannelById(channelId))
                .collectList()
                .flatMap(mentionedChannels -> {
                    if (mentionedChannels.isEmpty()) {
                        throw new CommandException(String.format("Channel `%s` not found.", args.get(2)));
                    }

                    return Mono.zip(Mono.just(mentionedChannels),
                            DatabaseManager.getGuilds().getDBGuild(context.getGuildId()));
                })
                .map(tuple -> {
                    final List<Channel> mentionedChannels = tuple.getT1();
                    final DBGuild dbGuild = tuple.getT2();

                    final List<Snowflake> allowedTextChannelIds = dbGuild.getSettings().getAllowedTextChannelIds();
                    final List<Snowflake> allowedVoiceChannelIds = dbGuild.getSettings().getAllowedVoiceChannelIds();

                    final List<Snowflake> mentionedVoiceChannelIds = mentionedChannels.stream()
                            .filter(channel -> channel.getType() == Channel.Type.GUILD_VOICE)
                            .map(Channel::getId)
                            .collect(Collectors.toList());

                    final List<Snowflake> mentionedTextChannelIds = mentionedChannels.stream()
                            .filter(channel -> channel.getType() == Channel.Type.GUILD_TEXT)
                            .map(Channel::getId)
                            .collect(Collectors.toList());

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
                            strBuilder.append("\n" + Emoji.INFO + " There are no more allowed text channels set, I can now speak in all the text channels.");
                        }
                        if (!mentionedVoiceChannelIds.isEmpty() && allowedVoiceChannelIds.isEmpty()) {
                            strBuilder.append("\n" + Emoji.INFO + " There are no more allowed voice channels set, I can now connect to all voice channels.");
                        }
                    }

                    dbGuild.setSetting(Setting.ALLOWED_TEXT_CHANNELS, allowedTextChannelIds);
                    dbGuild.setSetting(Setting.ALLOWED_VOICE_CHANNELS, allowedVoiceChannelIds);

                    return strBuilder.toString();
                })
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
