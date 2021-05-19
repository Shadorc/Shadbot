package com.locibot.locibot.listener;

import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.guilds.entity.DBMember;
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
    public Mono<?> execute(MemberLeaveEvent event) {
        // Delete the member from the database
        final Mono<Void> deleteMember = Mono.justOrEmpty(event.getMember())
                .flatMap(member -> DatabaseManager.getGuilds()
                        .getDBMember(member.getGuildId(), member.getId()))
                .flatMap(DBMember::delete);

        // Send an automatic leave message if one was configured
        @Deprecated final Mono<Message> sendLeaveMessageDeprecated = DatabaseManager.getGuilds()
                .getSettings(event.getGuildId())
                .flatMap(settings -> Mono.zip(
                        Mono.justOrEmpty(settings.getMessageChannelId()),
                        Mono.justOrEmpty(settings.getLeaveMessage())))
                .flatMap(TupleUtils.function((channelId, leaveMessage) ->
                        MemberJoinListener.sendAutoMessage(event.getClient(), event.getUser(), channelId, leaveMessage)));

        final Mono<Message> sendLeaveMessage = DatabaseManager.getGuilds()
                .getSettings(event.getGuildId())
                .flatMap(settings -> Mono.justOrEmpty(settings.getAutoLeaveMessage()))
                .flatMap(autoLeaveMessage ->
                        MemberJoinListener.sendAutoMessage(event.getClient(), event.getUser(),
                                autoLeaveMessage.getChannelId(), autoLeaveMessage.getMessage()));

        return deleteMember
                .and(sendLeaveMessageDeprecated)
                .and(sendLeaveMessage);
    }
}
