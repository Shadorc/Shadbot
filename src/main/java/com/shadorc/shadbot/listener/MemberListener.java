package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.db.guild.GuildManager;
import com.shadorc.shadbot.db.guild.entity.DBGuild;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class MemberListener {

    public static class MemberJoinListener implements EventListener<MemberJoinEvent> {

        @Override
        public Class<MemberJoinEvent> getEventType() {
            return MemberJoinEvent.class;
        }

        @Override
        public Mono<Void> execute(MemberJoinEvent event) {
            final DBGuild dbGuild = GuildManager.getInstance().getDBGuild(event.getGuildId());

            final Mono<Message> sendWelcomeMessage = Mono.zip(
                    Mono.justOrEmpty(dbGuild.getSettings().getMessageChannelId()),
                    Mono.justOrEmpty(dbGuild.getSettings().getJoinMessage()))
                    .flatMap(tuple -> MemberListener.sendAutoMessage(event.getClient(), event.getMember(), tuple.getT1(), tuple.getT2()));

            final Flux<Void> addAutoRoles = Mono.zip(
                    event.getGuild(),
                    Mono.justOrEmpty(event.getClient().getSelfId()))
                    .flatMap(tuple -> tuple.getT1().getMemberById(tuple.getT2()))
                    .flatMapMany(self -> self.getBasePermissions()
                            .filter(permissions -> permissions.contains(Permission.MANAGE_ROLES))
                            .flatMapMany(ignored -> Flux.fromIterable(dbGuild.getSettings().getAutoRoles())
                                    .map(Snowflake::of)
                                    .flatMap(roleId -> event.getClient().getRoleById(event.getGuildId(), roleId))
                                    .filterWhen(role -> self.hasHigherRoles(List.of(role)))
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
            final DBGuild dbGuild = GuildManager.getInstance().getDBGuild(event.getGuildId());

            event.getMember()
                    .ifPresent(member -> GuildManager.getInstance().getDBMember(member.getGuildId(), member.getId()).delete());

            final Mono<Message> sendLeaveMessage = Mono.zip(
                    Mono.justOrEmpty(dbGuild.getSettings().getMessageChannelId()),
                    Mono.justOrEmpty(dbGuild.getSettings().getLeaveMessage()))
                    .flatMap(tuple -> MemberListener.sendAutoMessage(event.getClient(), event.getUser(), tuple.getT1(), tuple.getT2()));

            return sendLeaveMessage.then();
        }
    }

    private static Mono<Message> sendAutoMessage(DiscordClient client, User user, Long channelId, String message) {
        return client.getChannelById(Snowflake.of(channelId))
                .cast(MessageChannel.class)
                .flatMap(channel -> DiscordUtils.sendMessage(message
                        .replace("{username}", user.getUsername())
                        .replace("{userId}", user.getId().asString())
                        .replace("{mention}", user.getMention()), channel));
    }

}
