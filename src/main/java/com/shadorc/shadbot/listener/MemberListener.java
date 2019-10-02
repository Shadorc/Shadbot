package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.db.database.DBGuild;
import com.shadorc.shadbot.db.database.DatabaseManager;
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
import reactor.util.annotation.Nullable;

import java.util.List;

public class MemberListener {

    public static class MemberJoinListener implements EventListener<MemberJoinEvent> {

        @Override
        public Class<MemberJoinEvent> getEventType() {
            return MemberJoinEvent.class;
        }

        @Override
        public Mono<Void> execute(MemberJoinEvent event) {
            final DBGuild dbGuild = DatabaseManager.getInstance().getDBGuild(event.getGuildId());
            return MemberListener.sendAutoMsg(event.getClient(), event.getMember(),
                    dbGuild.getMessageChannelId().orElse(null), dbGuild.getJoinMessage().orElse(null))
                    // Add auto-role(s) to the new member
                    .and(Mono.zip(event.getGuild(), Mono.justOrEmpty(event.getClient().getSelfId()))
                            .flatMap(tuple -> tuple.getT1().getMemberById(tuple.getT2()))
                            .flatMapMany(self -> self.getBasePermissions()
                                    .filter(permissions -> permissions.contains(Permission.MANAGE_ROLES))
                                    .flatMapMany(ignored -> Flux.fromIterable(dbGuild.getAutoRoles())
                                            .map(Snowflake::of)
                                            .flatMap(roleId -> event.getClient().getRoleById(event.getGuildId(), roleId))
                                            .filterWhen(role -> self.hasHigherRoles(List.of(role)))
                                            .flatMap(role -> event.getMember().addRole(role.getId())))));
        }
    }

    public static class MemberLeaveListener implements EventListener<MemberLeaveEvent> {

        @Override
        public Class<MemberLeaveEvent> getEventType() {
            return MemberLeaveEvent.class;
        }

        @Override
        public Mono<Void> execute(MemberLeaveEvent event) {
            final DBGuild dbGuild = DatabaseManager.getInstance().getDBGuild(event.getGuildId());
            event.getMember()
                    .ifPresent(member -> dbGuild.removeMember(DatabaseManager.getInstance().getDBMember(member.getGuildId(), member.getId())));
            return MemberListener.sendAutoMsg(event.getClient(), event.getUser(),
                    dbGuild.getMessageChannelId().orElse(null), dbGuild.getLeaveMessage().orElse(null))
                    .then();
        }
    }

    private static Mono<Message> sendAutoMsg(DiscordClient client, User user, @Nullable Long channelId, @Nullable String message) {
        return Mono.zip(Mono.justOrEmpty(channelId).map(Snowflake::of), Mono.justOrEmpty(message))
                .flatMap(tuple -> client.getChannelById(tuple.getT1())
                        .cast(MessageChannel.class)
                        .flatMap(channel -> DiscordUtils.sendMessage(tuple.getT2()
                                .replace("{username}", user.getUsername())
                                .replace("{userId}", user.getId().asString())
                                .replace("{mention}", user.getMention()), channel)));
    }

}
