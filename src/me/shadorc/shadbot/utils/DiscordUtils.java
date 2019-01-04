package me.shadorc.shadbot.utils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import org.apache.commons.lang3.BooleanUtils;
import org.reactivestreams.Publisher;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.PrivateChannel;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.exception.ExceptionHandler;
import me.shadorc.shadbot.core.exception.ExceptionUtils;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.VariousEnum;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingPermissionException;
import me.shadorc.shadbot.exception.MissingPermissionException.UserType;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class DiscordUtils {

	/* Audit Log reason must be 512 or fewer in length. */
	public static final int MAX_REASON_LENGTH = 512;

	public static Mono<Message> sendMessage(String content, MessageChannel channel) {
		return DiscordUtils.sendMessage(content, null, channel);
	}

	public static Mono<Message> sendMessage(EmbedCreateSpec embed, MessageChannel channel) {
		return DiscordUtils.sendMessage(null, embed, channel);
	}

	public static Mono<Message> sendMessage(String content, EmbedCreateSpec embed, MessageChannel channel) {
		final Snowflake selfId = channel.getClient().getSelfId().get();
		return Mono.zip(
				DiscordUtils.hasPermission(channel, selfId, Permission.SEND_MESSAGES),
				DiscordUtils.hasPermission(channel, selfId, Permission.EMBED_LINKS))
				.flatMap(tuple -> {
					final boolean canSendMessage = tuple.getT1();
					final boolean canSendEmbed = tuple.getT2();

					if(!canSendMessage) {
						LogUtils.info("{Channel ID: %d} Missing permission: %s",
								channel.getId().asLong(), StringUtils.capitalizeEnum(Permission.SEND_MESSAGES));
						return Mono.empty();
					}

					if(!canSendEmbed && embed != null) {
						LogUtils.info("{Channel ID: %d} Missing permission: %s",
								channel.getId().asLong(), StringUtils.capitalizeEnum(Permission.EMBED_LINKS));
						return DiscordUtils.sendMessage(String.format(Emoji.ACCESS_DENIED + " I cannot send embed links.%nPlease, check my permissions "
								+ "and channel-specific ones to verify that **%s** is checked.",
								StringUtils.capitalizeEnum(Permission.EMBED_LINKS)), channel);
					}

					return channel.createMessage(spec -> {
						if(content != null) {
							spec.setContent(content);
						}
						if(embed != null) {
							spec.setEmbed(embed);
						}
					});
				})
				.doOnNext(message -> {
					if(!message.getEmbeds().isEmpty()) {
						StatsManager.VARIOUS_STATS.log(VariousEnum.EMBEDS_SENT);
					}
					StatsManager.VARIOUS_STATS.log(VariousEnum.MESSAGES_SENT);
				})
				// TODO: Remove when this issue is closed: https://github.com/Discord4J/Discord4J/issues/468
				.onErrorResume(ExceptionUtils::isForbidden, err -> Mono.fromRunnable(() -> LogUtils.error("Forbidden action while sending message.")));
	}

	/**
	 * @param channel - the channel containing the messages to delete
	 * @param messages - the {@link List} of messages to delete
	 * @return The number of deleted messages
	 */
	public static Mono<Integer> bulkDelete(TextChannel channel, List<Message> messages) {
		switch (messages.size()) {
			case 1:
				return messages.get(0)
						.delete()
						.thenReturn(messages.size());
			default:
				return channel.bulkDelete(Flux.fromIterable(messages).map(Message::getId))
						.count()
						.map(messagesNotDeleted -> (int) (messages.size() - messagesNotDeleted));
		}
	}

	/**
	 * @param guild - a {@link Guild} containing the channels to extract
	 * @param content - a string containing channels mentions / names
	 * @return A {@link Snowflake} {@link Flux} containing the IDs of the extracted channels
	 */
	public static Flux<Snowflake> extractChannels(Guild guild, String content) {
		final List<String> words = StringUtils.split(content);
		return guild.getChannels()
				.filter(channel -> words.contains(String.format("#%s", channel.getName())) || words.contains(channel.getMention()))
				.map(GuildChannel::getId)
				.distinct();
	}

	/**
	 * @param guild - a {@link Guild} containing the roles to extract
	 * @param content - a string containing role mentions / names
	 * @return A {@link Snowflake} {@link Flux} containing the IDs of the extracted roles
	 */
	public static Flux<Snowflake> extractRoles(Guild guild, String content) {
		final List<String> words = StringUtils.split(content);
		return guild.getRoles()
				.filter(role -> words.contains(String.format("@%s", role.getName())) || words.contains(role.getMention()))
				.map(Role::getId)
				.distinct();
	}

	/**
	 * @param channelId - The ID of the channel to mention
	 * @return The channel mentionned
	 */
	public static String getChannelMention(Snowflake channelId) {
		return String.format("<#%d>", channelId.asLong());
	}

	/**
	 * @param message - the message
	 * @return The members mentioned in a {@link Message}
	 */
	public static Flux<Member> getMembersFrom(Message message) {
		if(message.mentionsEveryone()) {
			return message.getGuild().flatMapMany(Guild::getMembers);
		}
		return message.getGuild()
				.flatMapMany(Guild::getMembers)
				.filter(member -> message.getUserMentionIds().contains(member.getId())
						|| !Collections.disjoint(member.getRoleIds(), message.getRoleMentionIds()));
	}

	/**
	 * @param channel - the channel
	 * @param userId - the user ID
	 * @param permission - the permission
	 * @return Return true if the user has the permission in the channel, false otherwise
	 */
	public static Mono<Boolean> hasPermission(Channel channel, Snowflake userId, Permission permission) {
		// An user has all the permissions in a private channel
		if(channel instanceof PrivateChannel) {
			return Mono.just(true);
		}
		return GuildChannel.class.cast(channel).getEffectivePermissions(userId).map(permissions -> permissions.contains(permission));
	}

	/**
	 * @param client - the client on which register the event
	 * @param eventClass - the class of the event to register
	 * @param mapper - the mapper to execute when the event is triggered
	 */
	public static <T extends Event, R> void register(DiscordClient client, Class<T> eventClass, Function<? super T, ? extends Publisher<? extends R>> mapper) {
		client.getEventDispatcher()
				.on(eventClass)
				.flatMap(mapper)
				.onErrorContinue((err, obj) -> ExceptionHandler.handleUnknownError(err, client))
				.subscribe();
	}

	/**
	 * @param client - the client on which register the event
	 * @param mapper - the mapper to execute when the client is fully ready
	 */
	public static <R> void registerFullyReadyEvent(DiscordClient client, Function<? super GuildCreateEvent, ? extends Publisher<? extends R>> mapper) {
		client.getEventDispatcher().on(ReadyEvent.class)
				.map(event -> event.getGuilds().size())
				.flatMap(size -> client.getEventDispatcher()
						.on(GuildCreateEvent.class)
						.take(size)
						.last())
				.flatMap(mapper)
				.onErrorContinue((err, obj) -> ExceptionHandler.handleUnknownError(err, client))
				.subscribe();
	}

	/**
	 * @param member - the member who bet
	 * @param betStr - the string representing the bet
	 * @param maxValue - the maximum bet value
	 * @return An Integer representing {@code betStr} converted as an integer
	 * @throws CommandException - thrown if {@code betStr} cannot be casted to integer, if the {@code user} does not have enough coins or if the bet value
	 *             is superior to {code maxValue}
	 */
	public static int requireBet(Member member, String betStr, int maxValue) {
		final Integer bet = NumberUtils.asPositiveInt(betStr);
		if(bet == null) {
			throw new CommandException(String.format("`%s` is not a valid amount for coins.", betStr));
		}

		if(Shadbot.getDatabase().getDBMember(member.getGuildId(), member.getId()).getCoins() < bet) {
			throw new CommandException(TextUtils.NOT_ENOUGH_COINS);
		}

		if(bet > maxValue) {
			throw new CommandException(String.format("Sorry, you can't bet more than **%s**.",
					FormatUtils.coins(maxValue)));
		}

		return bet;
	}

	public static Mono<Void> requirePermissions(Channel channel, Snowflake userId, UserType userType, Permission... permissions) {
		return Flux.fromArray(permissions)
				.filterWhen(permission -> DiscordUtils.hasPermission(channel, userId, permission).map(BooleanUtils::negate))
				.flatMap(permission -> Mono.error(new MissingPermissionException(userType, permission)))
				.then();
	}

	/**
	 * @param context - the context
	 * @return The user voice channel ID if the user is in a voice channel and the bot is allowed to join or if the user is in a voice channel or if the
	 *         user and the bot are in the same voice channel
	 */
	public static Mono<Snowflake> requireSameVoiceChannel(Context context) {
		final Mono<Optional<Snowflake>> botVoiceChannelIdMono = context.getSelfAsMember()
				.flatMap(Member::getVoiceState)
				.map(VoiceState::getChannelId)
				.defaultIfEmpty(Optional.empty());

		final Mono<Optional<Snowflake>> userVoiceChannelIdMono = context.getMember()
				.getVoiceState()
				.map(VoiceState::getChannelId)
				.defaultIfEmpty(Optional.empty());

		return Mono.zip(botVoiceChannelIdMono, userVoiceChannelIdMono)
				.map(tuple -> {
					final Optional<Snowflake> botVoiceChannelId = tuple.getT1();
					final Optional<Snowflake> userVoiceChannelId = tuple.getT2();

					// If the user is in a voice channel but the bot is not allowed to join
					if(userVoiceChannelId.isPresent() && !Shadbot.getDatabase().getDBGuild(context.getGuildId()).isVoiceChannelAllowed(userVoiceChannelId.get())) {
						throw new CommandException("I'm not allowed to join this voice channel.");
					}

					// If the user and the bot are not in a voice channel
					if(!botVoiceChannelId.isPresent() && !userVoiceChannelId.isPresent()) {
						throw new CommandException("Join a voice channel before using this command.");
					}

					// If the user and the bot are not in the same voice channel
					if(botVoiceChannelId.isPresent() && !userVoiceChannelId.map(botVoiceChannelId.get()::equals).orElse(false)) {
						throw new CommandException(String.format("I'm currently playing music in voice channel <#%d>"
								+ ", join me before using this command.", botVoiceChannelId.map(Snowflake::asLong).get()));
					}

					return userVoiceChannelId.get();
				});
	}

	public static Mono<Void> updatePresence(DiscordClient client) {
		String text;
		if(ThreadLocalRandom.current().nextInt(2) == 0) {
			text = TextUtils.PLAYING.getText();
		} else {
			text = String.format("%shelp | %s", Config.DEFAULT_PREFIX, Utils.randValue(TextUtils.TIP_MESSAGES));
		}
		return client.updatePresence(Presence.online(Activity.playing(text)));

	}

}
