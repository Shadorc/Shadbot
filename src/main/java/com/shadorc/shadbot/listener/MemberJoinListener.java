package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.utils.DiscordUtils;
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

import java.util.Set;

public class MemberJoinListener implements EventListener<MemberJoinEvent> {

    @Override
    public Class<MemberJoinEvent> getEventType() {
        return MemberJoinEvent.class;
    }

    @Override
    public Mono<Void> execute(MemberJoinEvent event) {
        // Send an automatic join message if one was configured
        final Mono<Message> sendWelcomeMessage = DatabaseManager.getGuilds()
                .getDBGuild(event.getGuildId())
                .map(DBGuild::getSettings)
                .flatMap(settings -> Mono.zip(
                        Mono.justOrEmpty(settings.getMessageChannelId()),
                        Mono.justOrEmpty(settings.getJoinMessage())))
                .flatMap(tuple -> MemberJoinListener.sendAutoMessage(event.getClient(), event.getMember(),
                        tuple.getT1(), tuple.getT2()));

        // Add auto-roles when a user joins if they are configured
        final Flux<Void> addAutoRoles = event.getGuild()
                .flatMap(Guild::getSelfMember)
                .flatMapMany(self -> self.getBasePermissions()
                        .filter(permissions -> permissions.contains(Permission.MANAGE_ROLES))
                        .flatMap(ignored -> DatabaseManager.getGuilds()
                                .getDBGuild(event.getGuildId())
                                .map(DBGuild::getSettings))
                        .flatMapMany(settings -> Flux.fromIterable(settings.getAutoRoleIds())
                                .flatMap(roleId -> event.getClient().getRoleById(event.getGuildId(), roleId))
                                .filterWhen(role -> self.hasHigherRoles(Set.of(role.getId())))
                                .flatMap(role -> event.getMember().addRole(role.getId()))));

        return sendWelcomeMessage.and(addAutoRoles);
    }

    public static Mono<Message> sendAutoMessage(GatewayDiscordClient gateway, User user, Snowflake channelId, String message) {
        return gateway.getChannelById(channelId)
                .cast(MessageChannel.class)
                .flatMap(channel -> DiscordUtils.sendMessage(message
                        .replace("{username}", user.getUsername())
                        .replace("{userId}", user.getId().asString())
                        .replace("{mention}", user.getMention()), channel));
    }
}