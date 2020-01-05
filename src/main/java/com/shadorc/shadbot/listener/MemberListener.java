package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

public class MemberListener {

    public static class MemberJoinListener implements EventListener<MemberJoinEvent> {

        @Override
        public Class<MemberJoinEvent> getEventType() {
            return MemberJoinEvent.class;
        }

        @Override
        public Mono<Void> execute(MemberJoinEvent event) {
            final DBGuild dbGuild = DatabaseManager.getGuilds().getDBGuild(event.getGuildId());

            final Mono<Message> sendWelcomeMessage = Mono.zip(
                    Mono.justOrEmpty(dbGuild.getSettings().getMessageChannelId()),
                    Mono.justOrEmpty(dbGuild.getSettings().getJoinMessage()))
                    .flatMap(tuple -> MemberListener.sendAutoMessage(event.getClient(), event.getMember(), tuple.getT1(), tuple.getT2()));

            final Flux<Void> addAutoRoles = Mono.zip(
                    event.getGuild(),
                    event.getClient().getSelfId())
                    .flatMap(tuple -> tuple.getT1().getMemberById(tuple.getT2()))
                    .flatMapMany(self -> self.getBasePermissions()
                            .filter(permissions -> permissions.contains(Permission.MANAGE_ROLES))
                            .flatMapMany(ignored -> Flux.fromIterable(dbGuild.getSettings().getAutoRoleIds())
                                    .flatMap(roleId -> event.getClient().getRoleById(event.getGuildId(), roleId))
                                    .filterWhen(role -> self.hasHigherRoles(Set.of(role.getId())))
                                    .flatMap(role -> event.getMember().addRole(role.getId()))));

            return sendWelcomeMessage.and(addAutoRoles);
        }
    }

    public static class MemberLeaveListener implements EventListener<MemberLeaveEvent> {

        @Override
        public Class<MemberLeaveEvent> getEventType() {
            return MemberLeaveEvent.class;
        }

        @Override
        public Mono<Void> execute(MemberLeaveEvent event) {
            final DBGuild dbGuild = DatabaseManager.getGuilds().getDBGuild(event.getGuildId());

            event.getMember()
                    .ifPresent(member -> DatabaseManager.getGuilds().getDBMember(member.getGuildId(), member.getId()).delete());

            final Mono<Message> sendLeaveMessage = Mono.zip(
                    Mono.justOrEmpty(dbGuild.getSettings().getMessageChannelId()),
                    Mono.justOrEmpty(dbGuild.getSettings().getLeaveMessage()))
                    .flatMap(tuple -> MemberListener.sendAutoMessage(event.getClient(), event.getUser(), tuple.getT1(), tuple.getT2()));

            return sendLeaveMessage.then();
        }
    }

    private static Mono<Message> sendAutoMessage(GatewayDiscordClient client, User user, Snowflake channelId, String message) {
        return client.getChannelById(channelId)
                .cast(MessageChannel.class)
                .flatMap(channel -> DiscordUtils.sendMessage(message
                        .replace("{username}", user.getUsername())
                        .replace("{userId}", user.getId().asString())
                        .replace("{mention}", user.getMention()), channel));
    }

}
