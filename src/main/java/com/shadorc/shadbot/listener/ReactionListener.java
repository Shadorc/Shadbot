package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.command.moderation.IamCmd;
import com.shadorc.shadbot.core.i18n.I18nManager;
import com.shadorc.shadbot.database.DatabaseManager;
import com.shadorc.shadbot.database.guilds.entity.DBGuild;
import com.shadorc.shadbot.database.guilds.entity.Settings;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.message.TemporaryMessage;
import com.shadorc.shadbot.utils.FormatUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.Permission;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.Duration;
import java.util.Set;

public class ReactionListener {

    private static final Duration TMP_MESSAGE_DURATION = Duration.ofSeconds(8);

    private enum Action {
        ADD, REMOVE
    }

    public static class ReactionAddListener implements EventListener<ReactionAddEvent> {

        @Override
        public Class<ReactionAddEvent> getEventType() {
            return ReactionAddEvent.class;
        }

        @Override
        public Mono<?> execute(ReactionAddEvent event) {
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
        public Mono<?> execute(ReactionRemoveEvent event) {
            return event.getMessage()
                    .onErrorResume(ClientException.isStatusCode(HttpResponseStatus.FORBIDDEN.code()), err -> Mono.empty())
                    .flatMap(message -> ReactionListener.iam(message, event.getUserId(), event.getEmoji(), Action.REMOVE));
        }
    }

    private static Mono<Boolean> canManageRole(Message message, Snowflake roleId) {
        return message.getGuild()
                .flatMap(guild -> Mono.zip(
                        guild.getSelfMember(),
                        guild.getRoleById(roleId),
                        DatabaseManager.getGuilds().getDBGuild(guild.getId()).map(DBGuild::getLocale)))
                .flatMap(TupleUtils.function((selfMember, role, locale) -> Mono.zip(
                        selfMember.getBasePermissions().map(set -> set.contains(Permission.MANAGE_ROLES)),
                        selfMember.hasHigherRoles(Set.of(role.getId())))
                        .flatMap(TupleUtils.function((canManageRoles, hasHigherRoles) -> {
                            if (!canManageRoles) {
                                return new TemporaryMessage(message.getClient(), message.getChannelId(), TMP_MESSAGE_DURATION)
                                        .send(Emoji.ACCESS_DENIED, I18nManager.localize(locale, "iam.exception.permission.lack")
                                                .formatted(FormatUtil.capitalizeEnum(Permission.MANAGE_ROLES)))
                                        .thenReturn(false);
                            }

                            if (!hasHigherRoles) {
                                return new TemporaryMessage(message.getClient(), message.getChannelId(), TMP_MESSAGE_DURATION)
                                        .send(Emoji.ACCESS_DENIED, I18nManager.localize(locale, "iam.exception.hierarchy")
                                                .formatted(role.getName()))
                                        .thenReturn(false);
                            }

                            return Mono.just(true);
                        }))));
    }

    private static Mono<?> execute(Message message, Member member, Action action) {
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

    private static Mono<?> iam(Message message, Snowflake userId, ReactionEmoji emoji, Action action) {
        // If this is the correct reaction
        if (!emoji.equals(IamCmd.REACTION)) {
            return Mono.empty();
        }

        return Mono.just(message.getClient().getSelfId())
                // It wasn't the bot that reacted
                .filter(selfId -> !userId.equals(selfId))
                // If the bot is not the author of the message, this is not an Iam message
                // TODO: Use Message#getAuthor once https://github.com/Discord4J/Discord4J/issues/911 is fixed
                .filter(selfId -> message.getUserData().id().asLong() == selfId.asLong())
                .flatMap(__ -> message.getGuild().flatMap(guild -> guild.getMemberById(userId)))
                .flatMap(member -> ReactionListener.execute(message, member, action));
    }

}
