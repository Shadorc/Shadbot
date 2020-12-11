package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.command.admin.IamCmd;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.db.guilds.entity.Settings;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.message.TemporaryMessage;
import com.shadorc.shadbot.utils.FormatUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.Permission;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.Duration;
import java.util.Set;

public class ReactionListener {

    private enum Action {
        ADD, REMOVE
    }

    public static class ReactionAddListener implements EventListener<ReactionAddEvent> {

        @Override
        public Class<ReactionAddEvent> getEventType() {
            return ReactionAddEvent.class;
        }

        @Override
        public Mono<Void> execute(ReactionAddEvent event) {
            return event.getMessage()
                    .onErrorResume(ClientException.isStatusCode(HttpResponseStatus.FORBIDDEN.code()), err -> Mono.empty())
                    .flatMap(message -> ReactionListener.iam(message, event.getUserId(), event.getEmoji(), Action.ADD));
        }
    }

    public static class ReactionRemoveListener implements EventListener<ReactionRemoveEvent> {

        @Override
        public Class<ReactionRemoveEvent> getEventType() {
            return ReactionRemoveEvent.class;
        }

        @Override
        public Mono<Void> execute(ReactionRemoveEvent event) {
            return event.getMessage()
                    .onErrorResume(ClientException.isStatusCode(HttpResponseStatus.FORBIDDEN.code()), err -> Mono.empty())
                    .flatMap(message -> ReactionListener.iam(message, event.getUserId(), event.getEmoji(), Action.REMOVE));
        }
    }

    private static Mono<Boolean> canManageRole(Message message, Snowflake roleId) {
        return message.getGuild()
                .flatMap(guild -> Mono.zip(guild.getMemberById(message.getClient().getSelfId()), guild.getRoleById(roleId)))
                .flatMap(TupleUtils.function((selfMember, role) -> Mono.zip(
                        selfMember.getBasePermissions().map(set -> set.contains(Permission.MANAGE_ROLES)),
                        selfMember.hasHigherRoles(Set.of(role.getId())))
                        .flatMap(TupleUtils.function((canManageRoles, hasHigherRoles) -> {
                            if (!canManageRoles) {
                                return new TemporaryMessage(message.getClient(), message.getChannelId(), Duration.ofSeconds(15))
                                        .send(String.format(Emoji.ACCESS_DENIED
                                                        + " I can't add/remove a role due to a lack of permission."
                                                        + "%nPlease, check my permissions to verify that %s is checked.",
                                                String.format("**%s**", FormatUtils.capitalizeEnum(Permission.MANAGE_ROLES))))
                                        .thenReturn(false);
                            }

                            if (!hasHigherRoles) {
                                return new TemporaryMessage(message.getClient(), message.getChannelId(), Duration.ofSeconds(15))
                                        .send(String.format(Emoji.ACCESS_DENIED +
                                                        " I can't add/remove role `%s` because I'm lower or " +
                                                        "at the same level in the role hierarchy than this role.",
                                                role.getName()))
                                        .thenReturn(false);
                            }

                            return Mono.just(true);
                        }))));
    }

    private static Mono<Void> execute(Message message, Member member, Action action) {
        return DatabaseManager.getGuilds()
                .getDBGuild(member.getGuildId())
                .map(DBGuild::getSettings)
                .flatMapIterable(Settings::getIam)
                .filter(iam -> iam.getMessageId().equals(message.getId()))
                // If the bot can manage the role
                .filterWhen(iam -> ReactionListener.canManageRole(message, iam.getRoleId()))
                .flatMap(iam -> action == Action.ADD ? member.addRole(iam.getRoleId()) : member.removeRole(iam.getRoleId()))
                .then();
    }

    private static Mono<Void> iam(Message message, Snowflake userId, ReactionEmoji emoji, Action action) {
        // If this is the correct reaction
        if (!emoji.equals(IamCmd.REACTION)) {
            return Mono.empty();
        }

        return Mono.just(message.getClient().getSelfId())
                // It wasn't the bot that reacted
                .filter(selfId -> !userId.equals(selfId))
                // If the bot is not the author of the message, this is not an Iam message
                .filter(selfId -> message.getAuthor().map(User::getId).map(selfId::equals).orElse(false))
                .flatMap(ignored -> message.getGuild().flatMap(guild -> guild.getMemberById(userId)))
                .flatMap(member -> ReactionListener.execute(message, member, action));
    }

}
