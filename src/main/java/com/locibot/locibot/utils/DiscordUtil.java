package com.locibot.locibot.utils;

import com.locibot.locibot.LociBot;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.database.guilds.entity.Settings;
import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.command.MissingPermissionException;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.object.ExceptionHandler;
import discord4j.common.util.Snowflake;
import discord4j.core.object.VoiceState;
import discord4j.core.object.command.ApplicationCommandInteraction;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.*;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.AllowedMentions;
import discord4j.rest.util.Permission;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DiscordUtil {

    public static Mono<Message> sendMessage(Emoji emoji, String message, MessageChannel channel) {
        return DiscordUtil.sendMessage(emoji + " " + message, channel);
    }

    /**
     * @param content The string to send.
     * @param channel The {@link MessageChannel} in which to send the message.
     * @return A {@link Mono} where, upon successful completion, emits the created Message. If an error is received,
     * it is emitted through the Mono.
     */
    public static Mono<Message> sendMessage(String content, MessageChannel channel) {
        return DiscordUtil.sendMessage(spec -> spec.setContent(content), channel, false);
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
                DiscordUtil.hasPermission(channel, channel.getClient().getSelfId(), Permission.SEND_MESSAGES),
                DiscordUtil.hasPermission(channel, channel.getClient().getSelfId(), Permission.EMBED_LINKS))
                .flatMap(TupleUtils.function((canSendMessage, canSendEmbed) -> {
                    if (!canSendMessage) {
                        LociBot.DEFAULT_LOGGER.info("{Channel ID: {}} Missing permission: {}",
                                channel.getId().asLong(), FormatUtil.capitalizeEnum(Permission.SEND_MESSAGES));
                        return Mono.empty();
                    }

                    if (!canSendEmbed && hasEmbed) {
                        LociBot.DEFAULT_LOGGER.info("{Channel ID: {}} Missing permission: {}",
                                channel.getId().asLong(), FormatUtil.capitalizeEnum(Permission.EMBED_LINKS));
                        // TODO I18n
                        return DiscordUtil.sendMessage(Emoji.ACCESS_DENIED,
                                "I cannot send embed links.\nPlease, check my permissions and channel-specific ones to verify that **%s** is checked."
                                        .formatted(FormatUtil.capitalizeEnum(Permission.EMBED_LINKS)), channel);
                    }

                    return channel.createMessage(spec
                            .andThen(messageSpec -> messageSpec.setAllowedMentions(AllowedMentions.builder()
                                    .parseType(AllowedMentions.Type.ROLE, AllowedMentions.Type.USER)
                                    .build())));
                }))
                // 403 Forbidden means that the bot is not in the guild
                .onErrorResume(ClientException.isStatusCode(HttpResponseStatus.FORBIDDEN.code()), err -> Mono.empty())
                .timeout(Config.TIMEOUT)
                .retryWhen(ExceptionHandler.RETRY_ON_INTERNET_FAILURES.apply("Retries exhausted trying to send message"))
                .doOnSuccess(__ -> Telemetry.MESSAGE_SENT_COUNTER.inc());
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
                .flatMap(permission -> DiscordUtil.hasPermission(channel, channel.getClient().getSelfId(), permission)
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
        final Mono<Optional<Snowflake>> getBotVoiceChannelId = context.getGuild()
                .flatMap(Guild::getSelfMember)
                .flatMap(Member::getVoiceState)
                .map(VoiceState::getChannelId)
                .defaultIfEmpty(Optional.empty());

        final Mono<Optional<Snowflake>> getUserVoiceChannelId = context.getAuthor()
                .getVoiceState()
                .map(VoiceState::getChannelId)
                .defaultIfEmpty(Optional.empty());

        return Mono.zip(getBotVoiceChannelId, getUserVoiceChannelId)
                .map(TupleUtils.function((botVoiceChannelId, userVoiceChannelId) -> {
                    final Settings settings = context.getDbGuild().getSettings();

                    // If the user is in a voice channel but the bot is not allowed to join
                    if (userVoiceChannelId.isPresent()
                            && !settings.isVoiceChannelAllowed(userVoiceChannelId.get())) {
                        throw new CommandException(context.localize("voice.channel.not.allowed"));
                    }

                    // If the user and the bot are not in a voice channel
                    if (botVoiceChannelId.isEmpty() && userVoiceChannelId.isEmpty()) {
                        throw new CommandException(context.localize("no.voice.channel"));
                    }

                    // If the user and the bot are not in the same voice channel
                    if (botVoiceChannelId.isPresent() && !userVoiceChannelId.map(botVoiceChannelId.orElseThrow()::equals).orElse(false)) {
                        throw new CommandException(context.localize("different.voice.channel")
                                .formatted(botVoiceChannelId.map(Snowflake::asLong).orElseThrow()));
                    }

                    return userVoiceChannelId.orElseThrow();
                }))
                .flatMap(context.getClient()::getChannelById)
                .cast(VoiceChannel.class)
                .flatMap(channel -> DiscordUtil.requirePermissions(channel, Permission.CONNECT, Permission.SPEAK, Permission.VIEW_CHANNEL)
                        .thenReturn(channel));
    }

    /**
     * @param enumClass The enumeration to convert as a list of choices.
     * @param <T>       The type of enumeration.
     * @return An ordered list of {@link ApplicationCommandOptionChoiceData} converted from {@code enumClass}.
     */
    public static <T extends Enum<T>> List<ApplicationCommandOptionChoiceData> toOptions(Class<T> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .map(String::toLowerCase)
                .map(it -> ApplicationCommandOptionChoiceData.builder().name(it).value(it).build())
                .collect(Collectors.toList());
    }

    public static <T extends Enum<T>> List<ApplicationCommandOptionChoiceData> toOptions(Collection<String> list) {
        return list.stream()
                .map(it -> ApplicationCommandOptionChoiceData.builder().name(it).value(it).build())
                .collect(Collectors.toList());
    }

    public static List<ApplicationCommandInteractionOption> flattenOptions(ApplicationCommandInteraction interaction) {
        final ArrayList<ApplicationCommandInteractionOption> options = new ArrayList<>();
        DiscordUtil.flattenOptionRecursive(options, interaction.getOptions());
        return options;
    }

    private static void flattenOptionRecursive(List<ApplicationCommandInteractionOption> list,
                                               List<ApplicationCommandInteractionOption> options) {
        for (final ApplicationCommandInteractionOption option : options) {
            list.add(option);
            DiscordUtil.flattenOptionRecursive(list, option.getOptions());
        }
    }

}
