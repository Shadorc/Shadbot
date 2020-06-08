package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.db.guilds.entity.DBMember;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

public class MemberLeaveListener implements EventListener<MemberLeaveEvent> {

    @Override
    public Class<MemberLeaveEvent> getEventType() {
        return MemberLeaveEvent.class;
    }

    @Override
    public Mono<Void> execute(MemberLeaveEvent event) {
        // Delete the member from the database
        final Mono<Void> deleteMember = Mono.justOrEmpty(event.getMember())
                .flatMap(member -> DatabaseManager.getGuilds()
                        .getDBMember(member.getGuildId(), member.getId()))
                .flatMap(DBMember::delete);

        // Send an automatic leave message if one was configured
        final Mono<Message> sendLeaveMessage = DatabaseManager.getGuilds()
                .getDBGuild(event.getGuildId())
                .map(DBGuild::getSettings)
                .flatMap(settings -> Mono.zip(
                        Mono.justOrEmpty(settings.getMessageChannelId()),
                        Mono.justOrEmpty(settings.getLeaveMessage())))
                .flatMap(TupleUtils.function((channelId, leaveMessage) ->
                        MemberJoinListener.sendAutoMessage(event.getClient(), event.getUser(),
                                channelId, leaveMessage)));

        return deleteMember.and(sendLeaveMessage);
    }
}
