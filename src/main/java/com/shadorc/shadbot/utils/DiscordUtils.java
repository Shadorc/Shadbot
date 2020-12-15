package com.shadorc.shadbot.utils;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.command.MissingPermissionException;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.db.guilds.entity.Settings;
import com.shadorc.shadbot.object.Emoji;
import discord4j.common.util.Snowflake;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.*;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.AllowedMentions;
import discord4j.rest.util.Permission;
import io.netty.channel.unix.Errors;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.netty.http.client.PrematureCloseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class DiscordUtils {

    /**
     * @param content The string to send.
     * @param channel The {@link MessageChannel} in which to send the message.
     * @return A {@link Mono} where, upon successful completion, emits the created Message. If an error is received,
     * it is emitted through the Mono.
     */
    public static Mono<Message> sendMessage(String content, MessageChannel channel) {
        return DiscordUtils.sendMessage(spec -> spec.setContent(content), channel, false);
    }

    /**
     * @param embed   The {@link EmbedCreateSpec} consumer used to attach rich content when creating a message.
     * @param channel The {@link MessageChannel} in which to send the message.
     * @return A {@link Mono} where, upon successful completion, emits the created Message. If an error is received,
     * it is emitted through the Mono.
     */
    public static Mono<Message> sendMessage(Consumer<EmbedCreateSpec> embed, MessageChannel channel) {
        return DiscordUtils.sendMessage(spec -> spec.setEmbed(embed), channel, true);
    }

    /**
     * @param content The string to send.
     * @param embed   The {@link EmbedCreateSpec} consumer used to attach rich content when creating a message.
     * @param channel The {@link MessageChannel} in which to send the message.
     * @return A {@link Mono} where, upon successful completion, emits the created Message. If an error is received,
     * it is emitted through the Mono.
     */
    public static Mono<Message> sendMessage(String content, Consumer<EmbedCreateSpec> embed, MessageChannel channel) {
        return DiscordUtils.sendMessage(spec -> spec.setContent(content).setEmbed(embed), channel, true);
    }

    /**
     * @param spec     A {@link Consumer} that provides a "blank" {@link MessageCreateSpec} to be operated on.
     * @param channel  The {@link MessageChannel} in which to send the message.
     * @param hasEmbed Whether or not the spec contains an embed.
     * @return A {@link Mono} where, upon successful completion, emits the created Message. If an error is received,
     * it is emitted through the Mono.
     */
    public static Mono<Message> sendMessage(Consumer<MessageCreateSpec> spec, MessageChannel channel, boolean hasEmbed) {
        return Mono.zip(
                DiscordUtils.hasPermission(channel, channel.getClient().getSelfId(), Permission.SEND_MESSAGES),
                DiscordUtils.hasPermission(channel, channel.getClient().getSelfId(), Permission.EMBED_LINKS))
                .flatMap(TupleUtils.function((canSendMessage, canSendEmbed) -> {
                    if (!canSendMessage) {
                        DEFAULT_LOGGER.info("{Channel ID: {}} Missing permission: {}",
                                channel.getId().asLong(), FormatUtils.capitalizeEnum(Permission.SEND_MESSAGES));
                        return Mono.empty();
                    }

                    if (!canSendEmbed && hasEmbed) {
                        DEFAULT_LOGGER.info("{Channel ID: {}} Missing permission: {}",
                                channel.getId().asLong(), FormatUtils.capitalizeEnum(Permission.EMBED_LINKS));
                        return DiscordUtils.sendMessage(String.format(Emoji.ACCESS_DENIED + " I cannot send embed" +
                                        " links.%nPlease, check my permissions "
                                        + "and channel-specific ones to verify that **%s** is checked.",
                                FormatUtils.capitalizeEnum(Permission.EMBED_LINKS)), channel);
                    }

                    return channel.createMessage(spec
                            .andThen(messageSpec -> messageSpec.setAllowedMentions(AllowedMentions.builder()
                                    .parseType(AllowedMentions.Type.ROLE, AllowedMentions.Type.USER)
                                    .build())));
                }))
                // 403 Forbidden means that the bot is not in the guild
                .onErrorResume(ClientException.isStatusCode(HttpResponseStatus.FORBIDDEN.code()), err -> Mono.empty())
                .timeout(Duration.ofSeconds(15))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(err -> err instanceof PrematureCloseException
                                || err instanceof Errors.NativeIoException
                                || err instanceof TimeoutException));
    }

    public static Mono<Member> extractMemberOrAuthor(Guild guild, Message message) {
        final String[] args = message.getContent().split(" ");
        if (args.length == 1) {
            return message.getAuthorAsMember();
        }

        if (args.length > 2) {
            return Mono.error(new CommandException("You can't specify more than one user."));
        }

        return message.getUserMentions()
                .switchIfEmpty(DiscordUtils.extractMembers(guild, message.getContent()))
                .next()
                .flatMap(user -> user.asMember(guild.getId()))
                .switchIfEmpty(Mono.error(new CommandException(String.format("User **%s** not found.", args[1]))));
    }

    /**
     * @param guild The {@link Guild} containing the members to extract.
     * @param str   The string containing members mentions and / or names.
     * @return A {@link Member} {@link Flux} containing the extracted members.
     */
    public static Flux<Member> extractMembers(Guild guild, String str) {
        final List<String> words = StringUtils.split(str);
        return guild.getMembers()
                .filter(member -> words.contains(member.getDisplayName())
                        || words.contains(member.getUsername())
                        || words.contains(member.getTag())
                        || words.contains(member.getMention())
                        || words.contains(member.getNicknameMention()));
    }

    /**
     * @param guild The {@link Guild} containing the channels to extract.
     * @param str   The string containing channels mentions and / or names.
     * @return A {@link GuildChannel} {@link Flux} containing the extracted channels.
     */
    public static Flux<GuildChannel> extractChannels(Guild guild, String str) {
        final List<String> words = StringUtils.split(str);
        return guild.getChannels()
                .filter(channel -> words.contains(channel.getName())
                        || words.contains(String.format("#%s", channel.getName()))
                        || words.contains(channel.getMention()));
    }

    /**
     * @param guild The {@link Guild} containing the roles to extract.
     * @param str   The string containing role mentions and / or names.
     * @return A {@link Role} {@link Flux} containing the extracted roles.
     */
    public static Flux<Role> extractRoles(Guild guild, String str) {
        final List<String> words = StringUtils.split(str);
        return guild.getRoles()
                .filter(role -> words.contains(role.getName())
                        || words.contains(String.format("@%s", role.getName()))
                        || words.contains(role.getMention()));
    }

    /**
     * @param message The {@link Message} containing the members to extract.
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
     * @param channel    The channel in which the permission has to be checked.
     * @param userId     The ID of the user to check permissions for.
     * @param permission The permission to check.
     * @return {@code true} if the user has the permission in the provided channel, {@code false} otherwise.
     */
    public static Mono<Boolean> hasPermission(Channel channel, Snowflake userId, Permission permission) {
        // A user has all the permissions in a private channel
        if (channel instanceof PrivateChannel) {
            return Mono.just(true);
        }
        return ((GuildChannel) channel).getEffectivePermissions(userId)
                .map(permissions -> permissions.contains(permission));
    }

    /**
     * @param channel     The channel in which the permissions have to be checked.
     * @param permissions The permissions to check.
     * @return A {@link Mono} containing a {@link MissingPermissionException} if the bot does not have the provided
     * permissions in the provided channel or an empty Mono otherwise.
     */
    public static Mono<Void> requirePermissions(Channel channel, Permission... permissions) {
        return Flux.fromArray(permissions)
                .flatMap(permission -> DiscordUtils.hasPermission(channel, channel.getClient().getSelfId(), permission)
                        .filter(Boolean.TRUE::equals)
                        .switchIfEmpty(Mono.error(new MissingPermissionException(permission))))
                .then();
    }

    /**
     * @param context The context.
     * @return The user voice channel ID if the user is in a voice channel <b>AND</b> (the bot is allowed to join
     * <b>OR</b> if the user and the bot are in the same voice channel) <b>AND</b> the bot is able to view the voice channel,
     * connect and speak.
     */
    public static Mono<VoiceChannel> requireVoiceChannel(Context context) {
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
                .map(TupleUtils.function((botVoiceChannelId, userVoiceChannelId, settings) -> {
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
                    if (botVoiceChannelId.isPresent() && !userVoiceChannelId.map(botVoiceChannelId.orElseThrow()::equals).orElse(false)) {
                        throw new CommandException(String.format("I'm currently playing music in voice channel **<#%d>**"
                                        + ", join me before using this command.",
                                botVoiceChannelId.map(Snowflake::asLong).get()));
                    }

                    return userVoiceChannelId.orElseThrow();
                }))
                .flatMap(context.getClient()::getChannelById)
                .cast(VoiceChannel.class)
                .flatMap(channel -> DiscordUtils.requirePermissions(channel, Permission.CONNECT, Permission.SPEAK, Permission.VIEW_CHANNEL)
                        .thenReturn(channel));
    }

}
