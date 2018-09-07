package me.shadorc.shadbot.command.admin.setting;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import discord4j.core.object.entity.Channel.Type;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.setting.AbstractSetting;
import me.shadorc.shadbot.core.setting.Setting;
import me.shadorc.shadbot.core.setting.SettingEnum;
import me.shadorc.shadbot.data.db.DBGuild;
import me.shadorc.shadbot.data.db.DatabaseManager;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Setting(description = "Manage channels allowed to Shadbot.", setting = SettingEnum.ALLOWED_CHANNELS)
public class ChannelSetting extends AbstractSetting {

	private enum Action {
		ADD, REMOVE;
	}

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(3);
		args.remove(0);

		return context.getGuild()
				.flatMapMany(Guild::getChannels)
				.collectList()
				.flatMapMany(channels -> {
					final List<Snowflake> mentionedChannels = DiscordUtils.getChannelMentions(context.getContent());
					if(mentionedChannels.isEmpty()) {
						throw new MissingArgumentException();
					}

					final Action action = Utils.getEnum(Action.class, args.get(0));
					if(action == null) {
						throw new CommandException(String.format("`%s` is not a valid action. %s", args.get(0), FormatUtils.formatOptions(Action.class)));
					}

					final DBGuild dbGuild = DatabaseManager.getDBGuild(context.getGuildId());
					final Set<Snowflake> allowedTextChannels = new HashSet<>(dbGuild.getAllowedTextChannels());
					final Set<Snowflake> allowedVoiceChannels = new HashSet<>(dbGuild.getAllowedVoiceChannels());

					Flux<Message> messagesFlux = Flux.empty();
					if(Action.ADD.equals(action)) {
						if(allowedTextChannels.isEmpty()
								&& mentionedChannels.stream().noneMatch(context.getChannelId()::equals)) {
							messagesFlux = messagesFlux.concatWith(BotUtils.sendMessage(Emoji.WARNING + " You did not mentioned this channel. "
									+ "I will not reply here until this channel is added to the list of allowed channels.", context.getChannel()));
						}

						final List<Snowflake> textChannels = channels.stream()
								.filter(channel -> channel.getType().equals(Type.GUILD_TEXT))
								.map(GuildChannel::getId)
								.collect(Collectors.toList());
						final List<Snowflake> voiceChannels = channels.stream()
								.filter(channel -> channel.getType().equals(Type.GUILD_VOICE))
								.map(GuildChannel::getId)
								.collect(Collectors.toList());

						for(Snowflake channelId : mentionedChannels) {
							if(textChannels.contains(channelId)) {
								allowedTextChannels.add(channelId);
							} else if(voiceChannels.contains(channelId)) {
								allowedVoiceChannels.add(channelId);
							}
						}

						messagesFlux = messagesFlux.concatWith(BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Channel %s added to allowed channels.",
								FormatUtils.format(mentionedChannels, DiscordUtils::getChannelMention, ", ")), context.getChannel()));

					} else {
						allowedTextChannels.removeAll(mentionedChannels);
						allowedVoiceChannels.removeAll(mentionedChannels);
						messagesFlux = messagesFlux.concatWith(BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Channel %s removed from allowed channels.",
								FormatUtils.format(mentionedChannels, DiscordUtils::getChannelMention, ", ")), context.getChannel()));
					}

					dbGuild.setSetting(SettingEnum.ALLOWED_TEXT_CHANNELS, allowedTextChannels);
					dbGuild.setSetting(SettingEnum.ALLOWED_VOICE_CHANNELS, allowedVoiceChannels);
					return messagesFlux;
				})
				.then();
	}

	@Override
	public EmbedCreateSpec getHelp(Context context) {
		return EmbedUtils.getDefaultEmbed()
				.addField("Usage", String.format("`%s%s <action> <#channel(s)>`", context.getPrefix(), this.getCommandName()), false)
				.addField("Argument", String.format("**action** - %s%n**channel(s)** - the (voice) channel(s) to %s",
						FormatUtils.format(Action.class, "/"),
						FormatUtils.format(Action.class, "/")), false)
				.addField("Example", String.format("`%s%s add #general`", context.getPrefix(), this.getCommandName()), false);
	}

}
