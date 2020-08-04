package com.shadorc.shadbot.command.admin.member;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.command.MissingArgumentException;
import com.shadorc.shadbot.command.MissingPermissionException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.ratelimiter.RateLimiter;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.StringUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.audit.AuditLogEntry;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.Permission;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class RemoveMemberCmd extends BaseCmd {

    private final String conjugatedVerb;
    private final Permission permission;

    protected RemoveMemberCmd(String name, String conjugatedVerb, Permission permission) {
        super(CommandCategory.ADMIN, CommandPermission.ADMIN, List.of(name));
        this.setRateLimiter(new RateLimiter(2, Duration.ofSeconds(3)));

        this.conjugatedVerb = conjugatedVerb;
        this.permission = permission;
    }

    public abstract Mono<Void> action(Member member, String reason);

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        final Set<Snowflake> mentionedUserIds = context.getMessage().getUserMentionIds();
        if (mentionedUserIds.isEmpty()) {
            return Mono.error(new MissingArgumentException());
        }

        final Snowflake mentionUserId = new ArrayList<>(mentionedUserIds).get(0);
        if (mentionUserId.equals(context.getAuthorId())) {
            return Mono.error(new CommandException(String.format("You cannot %s yourself.", this.getName())));
        }

        if (mentionUserId.equals(context.getSelfId())) {
            return Mono.error(new CommandException(String.format("You cannot %s me.", this.getName())));
        }

        final StringBuilder reason = new StringBuilder(
                StringUtils.remove(arg,
                        String.format("<@%d>", mentionUserId.asLong()), String.format("<@!%d>", mentionUserId.asLong())));
        if (reason.length() == 0) {
            reason.append("Reason not specified.");
        }

        if (reason.length() > AuditLogEntry.MAX_REASON_LENGTH) {
            return Mono.error(new CommandException(
                    String.format("Reason cannot exceed **%d characters**.", AuditLogEntry.MAX_REASON_LENGTH)));
        }

        return Mono.zip(context.getGuild(),
                context.getChannel(),
                context.getSelfAsMember(),
                context.getClient().getMemberById(context.getGuildId(), mentionUserId))
                .filterWhen(tuple -> DiscordUtils.requirePermissions(tuple.getT2(), this.permission).thenReturn(true))
                .filterWhen(tuple -> this.canInteract(tuple.getT2(), tuple.getT3(), context.getMember(), tuple.getT4()))
                .flatMap(tuple -> this.sendMessage(tuple.getT1(), tuple.getT2(), context.getMember(), tuple.getT4(), reason.toString())
                        .then(this.action(tuple.getT4(), reason.toString()))
                        .then(DiscordUtils.sendMessage(String.format(Emoji.INFO + " **%s** %s **%s**. Reason: `%s`",
                                context.getUsername(), this.conjugatedVerb, tuple.getT4().getUsername(), reason),
                                tuple.getT2())))
                .onErrorMap(ClientException.isStatusCode(HttpResponseStatus.FORBIDDEN.code()),
                        err -> new MissingPermissionException(this.permission))
                .then();
    }

    private Mono<Boolean> canInteract(MessageChannel channel, Member self, Member author, Member memberToRemove) {
        return Mono.zip(self.isHigher(memberToRemove), author.isHigher(memberToRemove))
                .flatMap(TupleUtils.function((isSelfHigher, isAuthorHigher) -> {
                    if (!isSelfHigher) {
                        return DiscordUtils.sendMessage(
                                String.format(Emoji.WARNING + " (**%s**) I cannot %s **%s** because he is higher in " +
                                                "the role hierarchy than me.",
                                        author.getUsername(), this.getName(), memberToRemove.getUsername()), channel)
                                .thenReturn(false);
                    }
                    if (!isAuthorHigher) {
                        return DiscordUtils.sendMessage(
                                String.format(Emoji.WARNING + " (**%s**) You cannot %s **%s** because he is higher " +
                                                "in the role hierarchy than you.",
                                        author.getUsername(), this.getName(), memberToRemove.getUsername()), channel)
                                .thenReturn(false);
                    }
                    return Mono.just(true);
                }));
    }

    private Mono<Message> sendMessage(Guild guild, MessageChannel channel, Member author, Member memberToRemove, String reason) {
        if (memberToRemove.isBot()) {
            return Mono.empty();
        }
        return memberToRemove.getPrivateChannel()
                .flatMap(privateChannel -> DiscordUtils.sendMessage(
                        String.format(Emoji.WARNING + " You were %s from the server **%s** by **%s**. Reason: `%s`",
                                this.conjugatedVerb, guild.getName(), author.getUsername(), reason), privateChannel)
                        .switchIfEmpty(DiscordUtils.sendMessage(
                                String.format(Emoji.WARNING + " (**%s**) I could not send a message to **%s**.",
                                        author.getUsername(), memberToRemove.getUsername()), channel)));
    }

}
