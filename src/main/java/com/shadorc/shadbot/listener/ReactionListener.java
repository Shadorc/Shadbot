package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.command.admin.IamCmd;
import com.shadorc.shadbot.db.guild.GuildManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.message.TemporaryMessage;
import com.shadorc.shadbot.utils.StringUtils;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.rest.http.client.ClientException;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

public class ReactionListener {

    private enum Action {
        ADD, REMOVE;
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
                .flatMap(guild -> Mono.zip(guild.getMemberById(message.getClient().getSelfId().orElseThrow()),
                        guild.getRoleById(roleId)))
                .flatMap(tuple -> {
                    final Member selfMember = tuple.getT1();
                    final Role role = tuple.getT2();

                    return Mono.zip(selfMember.getBasePermissions().map(set -> set.contains(Permission.MANAGE_ROLES)),
                            selfMember.hasHigherRoles(List.of(role)))
                            .flatMap(tuple2 -> {
                                final boolean canManageRoles = tuple2.getT1();
                                final boolean hasHigherRoles = tuple2.getT2();

                                if (!canManageRoles) {
                                    return new TemporaryMessage(message.getClient(), message.getChannelId(), Duration.ofSeconds(15))
                                            .send(String.format(Emoji.ACCESS_DENIED
                                                            + " I can't add/remove a role due to a lack of permission."
                                                            + "%nPlease, check my permissions to verify that %s is checked.",
                                                    String.format("**%s**", StringUtils.capitalizeEnum(Permission.MANAGE_ROLES))))
                                            .thenReturn(false);
                                }

                                if (!hasHigherRoles) {
                                    return new TemporaryMessage(message.getClient(), message.getChannelId(), Duration.ofSeconds(15))
                                            .send(String.format(Emoji.ACCESS_DENIED +
                                                            " I can't add/remove role `%s` because I'm lower in the role hierarchy than this role.",
                                                    role.getName()))
                                            .thenReturn(false);
                                }

                                return Mono.just(true);
                            });
                });
    }

    private static Mono<Void> iam(Message message, Snowflake userId, ReactionEmoji emoji, Action action) {
        // If this is the correct reaction
        if (!emoji.equals(IamCmd.REACTION)) {
            return Mono.empty();
        }

        return Mono.justOrEmpty(message.getClient().getSelfId())
                // It wasn't the bot that reacted
                .filter(selfId -> !userId.equals(selfId))
                // If the bot is not the author of the message, this is not an Iam message
                .filter(selfId -> message.getAuthor().map(User::getId).map(selfId::equals).orElse(false))
                .flatMap(ignored -> message.getGuild().flatMap(guild -> guild.getMemberById(userId)))
                .flatMap(member -> Mono.justOrEmpty(GuildManager.getInstance()
                        .getDBGuild(member.getGuildId())
                        .getIamMessages()
                        .get(message.getId().asString()))
                        .map(Snowflake::of)
                        // If the bot can manage the role
                        .filterWhen(roleId -> ReactionListener.canManageRole(message, roleId))
                        .flatMap(roleId -> action == Action.ADD ? member.addRole(roleId) : member.removeRole(roleId)));
    }

}
