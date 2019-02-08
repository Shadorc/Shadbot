package me.shadorc.shadbot.utils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.lang3.BooleanUtils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import discord4j.common.jackson.PossibleModule;
import discord4j.common.jackson.UnknownPropertyHandler;
import discord4j.core.DiscordClient;
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
import discord4j.rest.RestClient;
import discord4j.rest.http.ExchangeStrategies;
import discord4j.rest.http.client.DiscordWebClient;
import discord4j.rest.json.response.GatewayResponse;
import discord4j.rest.request.DefaultRouter;
import discord4j.rest.route.Routes;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.VariousEnum;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingPermissionException;
import me.shadorc.shadbot.exception.MissingPermissionException.UserType;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

public class DiscordUtils {

	public static Mono<Message> sendMessage(String content, MessageChannel channel) {
		return DiscordUtils.sendMessage(content, null, channel);
	}

	public static Mono<Message> sendMessage(Consumer<EmbedCreateSpec> embed, MessageChannel channel) {
		return DiscordUtils.sendMessage(null, embed, channel);
	}

	public static Mono<Message> sendMessage(String content, Consumer<EmbedCreateSpec> embed, MessageChannel channel) {
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
				});
	}

	public static Mono<Integer> getRecommendedShardCount(String token) {
		final ObjectMapper mapper = new ObjectMapper()
				.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
				.addHandler(new UnknownPropertyHandler(false))
				.registerModules(new PossibleModule(), new Jdk8Module());

		final HttpHeaders defaultHeaders = new DefaultHttpHeaders();
		defaultHeaders.add(HttpHeaderNames.CONTENT_TYPE, "application/json");
		defaultHeaders.add(HttpHeaderNames.AUTHORIZATION, "Bot " + token);
		defaultHeaders.add(HttpHeaderNames.USER_AGENT, "DiscordBot(https://discord4j.com, v3)");

		final HttpClient httpClient = HttpClient.create().baseUrl(Routes.BASE_URL).compress(true);

		final DiscordWebClient webClient = new DiscordWebClient(httpClient, defaultHeaders,
				ExchangeStrategies.withJacksonDefaults(mapper));

		final RestClient restClient = new RestClient(new DefaultRouter(webClient));

		return restClient.getGatewayService().getGatewayBot().map(GatewayResponse::getShards);
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
	 * @param str - a string containing channels mentions / names
	 * @return A {@link Snowflake} {@link Flux} containing the IDs of the extracted channels
	 */
	public static Flux<Snowflake> extractChannels(Guild guild, String str) {
		final List<String> words = StringUtils.split(StringUtils.remove(str, "#"));
		return guild.getChannels()
				.filter(channel -> words.contains(String.format("%s", channel.getName())) || words.contains(channel.getMention()))
				.map(GuildChannel::getId)
				.distinct();
	}

	/**
	 * @param guild - a {@link Guild} containing the roles to extract
	 * @param str - a string containing role mentions / names
	 * @return A {@link Snowflake} {@link Flux} containing the IDs of the extracted roles
	 */
	public static Flux<Snowflake> extractRoles(Guild guild, String str) {
		final List<String> words = StringUtils.split(StringUtils.remove(str, "@"));
		return guild.getRoles()
				.filter(role -> words.contains(String.format("%s", role.getName())) || words.contains(role.getMention()))
				.map(Role::getId)
				.distinct();
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

	public static Mono<Void> requirePermissions(Channel channel, Snowflake userId, UserType userType, Permission... permissions) {
		return Flux.fromArray(permissions)
				.flatMap(permission -> DiscordUtils.hasPermission(channel, userId, permission)
						.filter(BooleanUtils::isTrue)
						.switchIfEmpty(Mono.error(new MissingPermissionException(userType, permission))))
				.then();
	}

	public static Mono<Boolean> isUserHigher(Guild guild, Member user1, Member user2) {
		if(guild.getOwnerId().equals(user1.getId())) {
			return Mono.just(true);
		}
		if(guild.getOwnerId().equals(user2.getId())) {
			return Mono.just(false);
		}

		return Mono.zip(user1.getRoles().collectList(), user2.getRoles().collectList())
				.flatMap(tuple -> hasHigherRoles(tuple.getT1(), tuple.getT2()));
	}

	private static Mono<Boolean> hasHigherRoles(List<Role> roles1, List<Role> roles2) {
		return Mono.zip(Flux.fromIterable(roles1).flatMap(Role::getPosition).sort().last().defaultIfEmpty(0),
				Flux.fromIterable(roles2).flatMap(Role::getPosition).sort().last().defaultIfEmpty(0))
				.map(tuple -> tuple.getT1() > tuple.getT2());
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
		return client.updatePresence(Presence.online(Activity.playing(
				String.format("%shelp | %s", Config.DEFAULT_PREFIX, Utils.randValue(TextUtils.TIP_MESSAGES)))));
	}

}
