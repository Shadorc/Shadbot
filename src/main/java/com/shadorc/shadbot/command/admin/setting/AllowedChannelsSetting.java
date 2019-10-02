package com.shadorc.shadbot.command.admin.setting;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.setting.BaseSetting;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.db.guild.DBGuild;
import com.shadorc.shadbot.db.guild.GuildManager;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Channel.Type;
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
                .map(mentionedChannels -> {
                    if (mentionedChannels.isEmpty()) {
                        throw new CommandException(String.format("Channel `%s` not found.", args.get(2)));
                    }

                    final DBGuild dbGuild = GuildManager.getInstance().getDBGuild(context.getGuildId());
                    final List<Long> allowedTextChannels = dbGuild.getAllowedTextChannels();
                    final List<Long> allowedVoiceChannels = dbGuild.getAllowedVoiceChannels();

                    final List<Long> mentionedVoiceChannelIds = mentionedChannels.stream()
                            .filter(channel -> channel.getType() == Type.GUILD_VOICE)
                            .map(Channel::getId)
                            .map(Snowflake::asLong)
                            .collect(Collectors.toList());

                    final List<Long> mentionedTextChannelIds = mentionedChannels.stream()
                            .filter(channel -> channel.getType() == Type.GUILD_TEXT)
                            .map(Channel::getId)
                            .map(Snowflake::asLong)
                            .collect(Collectors.toList());

                    final StringBuilder strBuilder = new StringBuilder();
                    if (action == Action.ADD) {
                        if (allowedTextChannels.isEmpty() && !mentionedTextChannelIds.isEmpty()
                                && mentionedTextChannelIds.stream().noneMatch(channelId -> channelId.equals(context.getChannelId().asLong()))) {
                            strBuilder.append(Emoji.WARNING + " You did not mentioned this channel. "
                                    + "I will not reply here until this channel is added to the list of allowed channels.\n");
                        }

                        for (final Channel channel : mentionedChannels) {
                            final long channelId = channel.getId().asLong();
                            if (channel.getType() == Type.GUILD_TEXT && !allowedTextChannels.contains(channelId)) {
                                allowedTextChannels.add(channelId);
                            } else if (channel.getType() == Type.GUILD_VOICE && !allowedVoiceChannels.contains(channelId)) {
                                allowedVoiceChannels.add(channelId);
                            }
                        }

                        strBuilder.append(String.format(Emoji.CHECK_MARK + " %s added to allowed channels.",
                                FormatUtils.format(mentionedChannels, Channel::getMention, ", ")));

                    } else {
                        allowedTextChannels.removeAll(mentionedTextChannelIds);
                        allowedVoiceChannels.removeAll(mentionedVoiceChannelIds);
                        strBuilder.append(String.format(Emoji.CHECK_MARK + " %s removed from allowed channels.",
                                FormatUtils.format(mentionedChannels, Channel::getMention, ", ")));

                        if (!mentionedTextChannelIds.isEmpty() && allowedTextChannels.isEmpty()) {
                            strBuilder.append("\n" + Emoji.INFO + " There are no more allowed text channels set, I can now speak in all the text channels.");
                        }
                        if (!mentionedVoiceChannelIds.isEmpty() && allowedVoiceChannels.isEmpty()) {
                            strBuilder.append("\n" + Emoji.INFO + " There are no more allowed voice channels set, I can now connect to all voice channels.");
                        }
                    }

                    dbGuild.setSetting(Setting.ALLOWED_TEXT_CHANNELS, allowedTextChannels);
                    dbGuild.setSetting(Setting.ALLOWED_VOICE_CHANNELS, allowedVoiceChannels);

                    return strBuilder.toString();
                })
                .flatMap(text -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(text, channel)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return DiscordUtils.getDefaultEmbed()
                .andThen(embed -> embed.addField("Usage", String.format("`%s%s <action> <#channel(s)>`", context.getPrefix(), this.getCommandName()), false)
                        .addField("Argument", String.format("**action** - %s%n**channel(s)** - the (voice) channel(s) to %s",
                                FormatUtils.format(Action.class, "/"),
                                FormatUtils.format(Action.class, "/")), false)
                        .addField("Example", String.format("`%s%s add #general`", context.getPrefix(), this.getCommandName()), false));
    }

}
