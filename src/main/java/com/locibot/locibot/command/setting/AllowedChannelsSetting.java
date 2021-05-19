package com.locibot.locibot.command.setting;

import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.core.command.*;
import com.locibot.locibot.core.command.*;
import com.locibot.locibot.database.guilds.entity.DBGuild;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.DiscordUtil;
import com.locibot.locibot.utils.FormatUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.Channel;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AllowedChannelsSetting extends BaseCmd {

    private enum Action {
        ADD, REMOVE
    }

    public AllowedChannelsSetting() {
        super(CommandCategory.SETTING, CommandPermission.ADMIN,
                "allowed_channels", "Manage channel(s) in which Shadbot can interact");

        this.addOption(option -> option.name("action")
                .description("Whether to add or remove a channel from the allowed ones")
                .type(ApplicationCommandOptionType.STRING.getValue())
                .required(true)
                .choices(DiscordUtil.toOptions(Action.class)));
        this.addOption(option -> option.name("channel1")
                .description("The first channel")
                .required(true)
                .type(ApplicationCommandOptionType.CHANNEL.getValue()));
        this.addOption(option -> option.name("channel2")
                .description("The second channel")
                .required(false)
                .type(ApplicationCommandOptionType.CHANNEL.getValue()));
        this.addOption(option -> option.name("channel3")
                .description("The third channel")
                .required(false)
                .type(ApplicationCommandOptionType.CHANNEL.getValue()));
        this.addOption(option -> option.name("channel4")
                .description("The fourth channel")
                .required(false)
                .type(ApplicationCommandOptionType.CHANNEL.getValue()));
        this.addOption(option -> option.name("channel5")
                .description("The fifth channel")
                .required(false)
                .type(ApplicationCommandOptionType.CHANNEL.getValue()));
    }

    @Override
    public Mono<?> execute(Context context) {
        final Action action = context.getOptionAsEnum(Action.class, "action").orElseThrow();
        final Flux<Channel> getChannels = Flux.fromStream(IntStream.range(1, 6).boxed())
                .flatMap(index -> context.getOptionAsChannel("channel%d".formatted(index)));

        return getChannels
                .collectList()
                .flatMap(mentionedChannels -> {
                    final DBGuild dbGuild = context.getDbGuild();

                    final Set<Snowflake> allowedTextChannelIds = dbGuild.getSettings().getAllowedTextChannelIds();
                    final Set<Snowflake> mentionedVoiceChannelIds = mentionedChannels.stream()
                            .filter(channel -> channel.getType() == Channel.Type.GUILD_VOICE)
                            .map(Channel::getId)
                            .collect(Collectors.toUnmodifiableSet());

                    final Set<Snowflake> allowedVoiceChannelIds = dbGuild.getSettings().getAllowedVoiceChannelIds();
                    final Set<Snowflake> mentionedTextChannelIds = mentionedChannels.stream()
                            .filter(channel -> channel.getType() == Channel.Type.GUILD_TEXT)
                            .map(Channel::getId)
                            .collect(Collectors.toUnmodifiableSet());

                    if (mentionedTextChannelIds.isEmpty() && mentionedVoiceChannelIds.isEmpty()) {
                        return Mono.error(new CommandException(context.localize("allowedchannels.missing.channels")));
                    }

                    final StringBuilder strBuilder = new StringBuilder();
                    if (action == Action.ADD) {
                        if (allowedTextChannelIds.isEmpty() && !mentionedTextChannelIds.isEmpty()
                                && mentionedTextChannelIds.stream().noneMatch(channelId -> channelId.equals(context.getChannelId()))) {
                            strBuilder.append(Emoji.WARNING + context.localize("allowedchannels.warning"));
                        }

                        for (final Channel channel : mentionedChannels) {
                            final Snowflake channelId = channel.getId();
                            switch (channel.getType()) {
                                case GUILD_TEXT -> allowedTextChannelIds.add(channelId);
                                case GUILD_VOICE -> allowedVoiceChannelIds.add(channelId);
                            }
                        }

                        strBuilder.append(Emoji.CHECK_MARK + context.localize("allowedchannels.added")
                                .formatted(FormatUtil.format(mentionedChannels, Channel::getMention, ", ")));

                    } else {
                        allowedTextChannelIds.removeAll(mentionedTextChannelIds);
                        allowedVoiceChannelIds.removeAll(mentionedVoiceChannelIds);
                        strBuilder.append(Emoji.CHECK_MARK + context.localize("allowedchannels.removed")
                                .formatted(FormatUtil.format(mentionedChannels, Channel::getMention, ", ")));

                        if (!mentionedTextChannelIds.isEmpty() && allowedTextChannelIds.isEmpty()) {
                            strBuilder.append("\n" + Emoji.INFO + context.localize("allowedchannels.text.reset"));
                        }
                        if (!mentionedVoiceChannelIds.isEmpty() && allowedVoiceChannelIds.isEmpty()) {
                            strBuilder.append("\n" + Emoji.INFO + context.localize("allowedchannels.voice.reset"));
                        }
                    }

                    return dbGuild.updateSetting(Setting.ALLOWED_TEXT_CHANNELS, allowedTextChannelIds)
                            .then(dbGuild.updateSetting(Setting.ALLOWED_VOICE_CHANNELS, allowedVoiceChannelIds))
                            .then(context.createFollowupMessage(strBuilder.toString()));
                });
    }

}