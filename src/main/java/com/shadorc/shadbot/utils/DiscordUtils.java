package com.shadorc.shadbot.utils;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.db.guilds.entity.Settings;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.exception.MissingPermissionException;
import com.shadorc.shadbot.object.Emoji;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.http.client.ClientException;
import io.netty.channel.unix.Errors;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;
import reactor.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class DiscordUtils {

    public static Mono<Message> sendMessage(String content, MessageChannel channel) {
        return DiscordUtils.sendMessage(spec -> spec.setContent(content), channel, false);
    }

    public static Mono<Message> sendMessage(Consumer<EmbedCreateSpec> embed, MessageChannel channel) {
        return DiscordUtils.sendMessage(spec -> spec.setEmbed(embed), channel, true);
    }

    public static Mono<Message> sendMessage(String content, Consumer<EmbedCreateSpec> embed, MessageChannel channel) {
        return DiscordUtils.sendMessage(spec -> spec.setContent(content).setEmbed(embed), channel, true);
    }

    public static Mono<Message> sendMessage(Consumer<MessageCreateSpec> spec, MessageChannel channel, boolean hasEmbed) {
        return Mono.zip(
                DiscordUtils.hasPermission(channel, Shadbot.getSelfId(), Permission.SEND_MESSAGES),
                DiscordUtils.hasPermission(channel, Shadbot.getSelfId(), Permission.EMBED_LINKS))
                .flatMap(tuple -> {
                    final boolean canSendMessage = tuple.getT1();
                    final boolean canSendEmbed = tuple.getT2();

                    if (!canSendMessage) {
                        LogUtils.info("{Channel ID: %d} Missing permission: %s",
                                channel.getId().asLong(), StringUtils.capitalizeEnum(Permission.SEND_MESSAGES));
                        return Mono.empty();
                    }

                    if (!canSendEmbed && hasEmbed) {
                        LogUtils.info("{Channel ID: %d} Missing permission: %s",
                                channel.getId().asLong(), StringUtils.capitalizeEnum(Permission.EMBED_LINKS));
                        return DiscordUtils.sendMessage(String.format(Emoji.ACCESS_DENIED + " I cannot send embed" +
                                        " links.%nPlease, check my permissions "
                                        + "and channel-specific ones to verify that **%s** is checked.",
                                StringUtils.capitalizeEnum(Permission.EMBED_LINKS)), channel);
                    }

                    return channel.createMessage(spec);
                })
                // 403 Forbidden means that the bot is not in the guild
                .onErrorResume(ClientException.isStatusCode(HttpResponseStatus.FORBIDDEN.code()), err -> Mono.empty())
                .retryWhen(Retry.onlyIf(err -> err.exception() instanceof PrematureCloseException || err.exception() instanceof Errors.NativeIoException)
                        .exponentialBackoff(Duration.ofSeconds(1), Duration.ofSeconds(5))
                        .retryMax(3));
    }

    /**
     * @param guild a {@link Guild} containing the channels to extract
     * @param str   a string containing channels mentions and / or names
     * @return A {@link Snowflake} {@link Flux} containing the IDs of the extracted channels.
     */
    public static Flux<Snowflake> extractChannels(Guild guild, String str) {
        final List<String> words = StringUtils.split(str);
        return guild.getChannels()
                .filter(channel -> words.contains(String.format("%s", channel.getName()))
                        || words.contains(String.format("#%s", channel.getName()))
                        || words.contains(channel.getMention()))
                .map(GuildChannel::getId)
                .distinct();
    }

    /**
     * @param guild a {@link Guild} containing the roles to extract
     * @param str   a string containing role mentions and / or names
     * @return A {@link Snowflake} {@link Flux} containing the IDs of the extracted roles.
     */
    public static Flux<Snowflake> extractRoles(Guild guild, String str) {
        final List<String> words = StringUtils.split(str);
        return guild.getRoles()
                .filter(role -> words.contains(String.format("%s", role.getName()))
                        || words.contains(String.format("@%s", role.getName()))
                        || words.contains(role.getMention()))
                .map(Role::getId)
                .distinct();
    }

    /**
     * @param message the {@link Message} containing the members to extract
     * @return A {@link Member} {@link Flux} mentioned in the {@link Message}.
     */
    public static Flux<Member> getMembersFrom(Message message) {
        if (message.mentionsEveryone()) {
            return message.getGuild().flatMapMany(Guild::getMembers);
        }
        return message.getGuild()
                .flatMapMany(Guild::getMembers)
                .filter(member -> message.getUserMentionIds().contains(member.getId())
                        || !Collections.disjoint(member.getRoleIds(), message.getRoleMentionIds()));
    }

    /**
     * @param channel    the channel
     * @param userId     the user ID
     * @param permission the permission
     * @return {@code true} if the user has the permission in the channel, {@code false} otherwise.
     */
    public static Mono<Boolean> hasPermission(Channel channel, Snowflake userId, Permission permission) {
        // An user has all the permissions in a private channel
        if (channel instanceof PrivateChannel) {
            return Mono.just(true);
        }
        return ((GuildChannel) channel).getEffectivePermissions(userId)
                .map(permissions -> permissions.contains(permission));
    }

    public static Mono<Void> requirePermissions(Channel channel, Permission... permissions) {
        return Flux.fromArray(permissions)
                .flatMap(permission -> DiscordUtils.hasPermission(channel, Shadbot.getSelfId(), permission)
                        .filter(Boolean.TRUE::equals)
                        .switchIfEmpty(Mono.error(new MissingPermissionException(permission))))
                .then();
    }

    /**
     * @param context the context
     * @return The user voice channel ID if the user is in a voice channel <b>AND</b> the bot is allowed to join
     * <b>OR</b> if the user and the bot are in the same voice channel.
     */
    public static Mono<Snowflake> requireSameVoiceChannel(Context context) {
        final Mono<Optional<Snowflake>> getBotVoiceChannelId = context.getSelfAsMember()
                .flatMap(Member::getVoiceState)
                .map(VoiceState::getChannelId)
                .defaultIfEmpty(Optional.empty());

        final Mono<Optional<Snowflake>> getUserVoiceChannelId = context.getMember()
                .getVoiceState()
                .map(VoiceState::getChannelId)
                .defaultIfEmpty(Optional.empty());

        final Mono<Settings> getSetting = DatabaseManager.getGuilds()
                .getDBGuild(context.getGuildId())
                .map(DBGuild::getSettings);

        return Mono.zip(getBotVoiceChannelId, getUserVoiceChannelId, getSetting)
                .map(tuple -> {
                    final Optional<Snowflake> botVoiceChannelId = tuple.getT1();
                    final Optional<Snowflake> userVoiceChannelId = tuple.getT2();
                    final Settings settings = tuple.getT3();

                    // If the user is in a voice channel but the bot is not allowed to join
                    if (userVoiceChannelId.isPresent()
                            && !settings.isVoiceChannelAllowed(userVoiceChannelId.get())) {
                        throw new CommandException("I'm not allowed to join this voice channel.");
                    }

                    // If the user and the bot are not in a voice channel
                    if (botVoiceChannelId.isEmpty() && userVoiceChannelId.isEmpty()) {
                        throw new CommandException("Join a voice channel before using this command.");
                    }

                    // If the user and the bot are not in the same voice channel
                    if (botVoiceChannelId.isPresent() && !userVoiceChannelId.map(botVoiceChannelId.get()::equals).orElse(false)) {
                        throw new CommandException(String.format("I'm currently playing music in voice channel <#%d>"
                                + ", join me before using this command.", botVoiceChannelId.map(Snowflake::asLong).get()));
                    }

                    return userVoiceChannelId.get();
                });
    }

    public static Consumer<EmbedCreateSpec> getDefaultEmbed() {
        return spec -> spec.setColor(Config.BOT_COLOR);
    }

}
