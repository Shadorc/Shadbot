package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.database.DatabaseManager;
import com.shadorc.shadbot.utils.DiscordUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.Set;

public class MemberJoinListener implements EventListener<MemberJoinEvent> {

    @Override
    public Class<MemberJoinEvent> getEventType() {
        return MemberJoinEvent.class;
    }

    @Override
    public Mono<?> execute(MemberJoinEvent event) {
        // Send an automatic join message if one was configured
        @Deprecated final Mono<Message> sendWelcomeMessageDeprecated = DatabaseManager.getGuilds()
                .getSettings(event.getGuildId())
                .flatMap(settings -> Mono.zip(
                        Mono.justOrEmpty(settings.getMessageChannelId()),
                        Mono.justOrEmpty(settings.getJoinMessage())))
                .flatMap(TupleUtils.function((messageChannelId, joinMessage) ->
                        MemberJoinListener.sendAutoMessage(event.getClient(), event.getMember(), messageChannelId, joinMessage)));

        final Mono<Message> sendWelcomeMessage = DatabaseManager.getGuilds()
                .getSettings(event.getGuildId())
                .flatMap(settings -> Mono.justOrEmpty(settings.getAutoJoinMessage()))
                .flatMap(autoJoinMessage ->
                        MemberJoinListener.sendAutoMessage(event.getClient(), event.getMember(),
                                autoJoinMessage.getChannelId(), autoJoinMessage.getMessage()));

        // Add auto-roles when a user joins if they are configured
        final Flux<Void> addAutoRoles = event.getGuild()
                .flatMap(Guild::getSelfMember)
                .flatMapMany(self -> self.getBasePermissions()
                        .filter(permissions -> permissions.contains(Permission.MANAGE_ROLES))
                        .flatMap(__ -> DatabaseManager.getGuilds().getSettings(event.getGuildId()))
                        .flatMapMany(settings -> Flux.fromIterable(settings.getAutoRoleIds())
                                .flatMap(roleId -> event.getClient().getRoleById(event.getGuildId(), roleId))
                                .filterWhen(role -> self.hasHigherRoles(Set.of(role.getId())))
                                .flatMap(role -> event.getMember().addRole(role.getId()))));

        return sendWelcomeMessageDeprecated
                .and(sendWelcomeMessage)
                .and(addAutoRoles);
    }

    public static Mono<Message> sendAutoMessage(GatewayDiscordClient gateway, User user, Snowflake channelId, String message) {
        return gateway.getChannelById(channelId)
                .cast(MessageChannel.class)
                .flatMap(channel -> DiscordUtil.sendMessage(message
                        .replace("{username}", user.getUsername())
                        .replace("{userId}", user.getId().asString())
                        .replace("{mention}", user.getMention()), channel));
    }
}