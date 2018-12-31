package me.shadorc.shadbot.command.admin.setting;

import java.util.List;
import java.util.stream.Collectors;

import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Channel.Type;
import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.setting.AbstractSetting;
import me.shadorc.shadbot.core.setting.Setting;
import me.shadorc.shadbot.core.setting.SettingEnum;
import me.shadorc.shadbot.data.database.DBGuild;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@Setting(description = "Manage channels allowed to Shadbot.", setting = SettingEnum.ALLOWED_CHANNELS)
public class AllowedChannelsSetting extends AbstractSetting {

	private enum Action {
		ADD, REMOVE;
	}

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(3);
		return context.getGuild()
				.flatMap(guild -> Mono.zip(guild.getChannels().collectList(), DiscordUtils.extractChannels(guild, context.getContent()).collectList()))
				.flatMap(tuple -> {
					final List<GuildChannel> channels = tuple.getT1();
					final List<Snowflake> mentionedChannels = tuple.getT2();
					if(mentionedChannels.isEmpty()) {
						throw new CommandException(String.format("`%s` is/are not valid channel(s).", args.get(2)));
					}

					final Action action = Utils.getEnum(Action.class, args.get(1));
					if(action == null) {
						throw new CommandException(String.format("`%s` is not a valid action. %s", args.get(1), FormatUtils.options(Action.class)));
					}

					final DBGuild dbGuild = Shadbot.getDatabase().getDBGuild(context.getGuildId());
					final List<Snowflake> allowedTextChannels = dbGuild.getAllowedTextChannels();
					final List<Snowflake> allowedVoiceChannels = dbGuild.getAllowedVoiceChannels();

					final StringBuilder strBuilder = new StringBuilder();
					if(Action.ADD.equals(action)) {
						if(allowedTextChannels.isEmpty()
								&& mentionedChannels.stream().noneMatch(context.getChannelId()::equals)) {
							strBuilder.append(Emoji.WARNING + " You did not mentioned this channel. "
									+ "I will not reply here until this channel is added to the list of allowed channels.\n");
						}

						final List<Snowflake> textChannels = channels.stream()
								.filter(channel -> channel.getType().equals(Type.GUILD_TEXT))
								.map(Channel::getId)
								.collect(Collectors.toList());
						final List<Snowflake> voiceChannels = channels.stream()
								.filter(channel -> channel.getType().equals(Type.GUILD_VOICE))
								.map(Channel::getId)
								.collect(Collectors.toList());

						for(final Snowflake channelId : mentionedChannels) {
							if(textChannels.contains(channelId) && !allowedTextChannels.contains(channelId)) {
								allowedTextChannels.add(channelId);
							} else if(voiceChannels.contains(channelId) && !allowedVoiceChannels.contains(channelId)) {
								allowedVoiceChannels.add(channelId);
							}
						}

						strBuilder.append(String.format(Emoji.CHECK_MARK + " Channel %s added to allowed channels.",
								FormatUtils.format(mentionedChannels, DiscordUtils::getChannelMention, ", ")));

					} else {
						allowedTextChannels.removeAll(mentionedChannels);
						allowedVoiceChannels.removeAll(mentionedChannels);
						strBuilder.append(String.format(Emoji.CHECK_MARK + " Channel %s removed from allowed channels.",
								FormatUtils.format(mentionedChannels, DiscordUtils::getChannelMention, ", ")));
					}

					dbGuild.setSetting(SettingEnum.ALLOWED_TEXT_CHANNELS, allowedTextChannels);
					dbGuild.setSetting(SettingEnum.ALLOWED_VOICE_CHANNELS, allowedVoiceChannels);
					return context.getChannel().flatMap(channel -> BotUtils.sendMessage(strBuilder.toString(), channel));
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
