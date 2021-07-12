package com.shadorc.shadbot.command.moderation.member;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.command.MissingPermissionException;
import com.shadorc.shadbot.core.command.*;
import com.shadorc.shadbot.core.i18n.I18nContext;
import com.shadorc.shadbot.core.i18n.I18nManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import discord4j.core.object.audit.AuditLogEntry;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.ApplicationCommandOptionType;
import discord4j.rest.util.Permission;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.Locale;
import java.util.stream.IntStream;

public abstract class RemoveMembersCmd extends SubCmd {

    private final Permission permission;
    private final String keyword;

    public RemoveMembersCmd(GroupCmd groupCmd, Permission permission, String keyword, String description) {
        super(groupCmd, CommandCategory.MODERATION, CommandPermission.ADMIN, keyword, description);
        this.permission = permission;
        this.keyword = keyword;

        this.addOption(option -> option.name("user1")
                .description("The first user to %s".formatted(keyword))
                .required(true)
                .type(ApplicationCommandOptionType.USER.getValue()));
        this.addOption(option -> option.name("user2")
                .description("The second user to %s".formatted(keyword))
                .required(false)
                .type(ApplicationCommandOptionType.USER.getValue()));
        this.addOption(option -> option.name("user3")
                .description("The third user to %s".formatted(keyword))
                .required(false)
                .type(ApplicationCommandOptionType.USER.getValue()));
        this.addOption(option -> option.name("user4")
                .description("The fourth user to %s".formatted(keyword))
                .required(false)
                .type(ApplicationCommandOptionType.USER.getValue()));
        this.addOption(option -> option.name("user5")
                .description("The fifth user to %s".formatted(keyword))
                .required(false)
                .type(ApplicationCommandOptionType.USER.getValue()));
        this.addOption(option -> option.name("user6")
                .description("The sixth user to %s".formatted(keyword))
                .required(false)
                .type(ApplicationCommandOptionType.USER.getValue()));
        this.addOption(option -> option.name("user7")
                .description("The seventh user to %s".formatted(keyword))
                .required(false)
                .type(ApplicationCommandOptionType.USER.getValue()));
        this.addOption(option -> option.name("user8")
                .description("The eighth user to %s".formatted(keyword))
                .required(false)
                .type(ApplicationCommandOptionType.USER.getValue()));
        this.addOption(option -> option.name("user9")
                .description("The ninth user to %s".formatted(keyword))
                .required(false)
                .type(ApplicationCommandOptionType.USER.getValue()));
        this.addOption(option -> option.name("reason")
                .description("The reason")
                .required(false)
                .type(ApplicationCommandOptionType.STRING.getValue()));
    }

    @Override
    public Mono<?> execute(Context context) {
        final String reason = context.getOptionAsString("reason").orElse("Reason not specified.");
        if (reason.length() > AuditLogEntry.MAX_REASON_LENGTH) {
            return Mono.error(new CommandException(
                    context.localize("exception.max.reason.length").formatted(AuditLogEntry.MAX_REASON_LENGTH)));
        }

        final Flux<Member> getUsers = Flux.fromStream(IntStream.range(1, 11).boxed())
                .flatMap(index -> context.getOptionAsMember("user%d".formatted(index)))
                .cache();

        return context.getChannel()
                .flatMap(channel -> DiscordUtil.requirePermissions(channel, this.permission))
                .then(Mono.zip(context.getClient().getSelfMember(context.getGuildId()), context.getGuild()))
                .flatMapMany(TupleUtils.function((self, guild) -> getUsers
                        .flatMap(user -> this.canInteract(context.getLocale(), self, context.getAuthor(), user)
                                .then(this.sendMessage(context, guild, context.getAuthor(), user, reason))
                                .then(this.action(user, reason))
                                .then(context.createFollowupMessage(Emoji.INFO, context.localize("%s.message".formatted(this.keyword))
                                        .formatted(context.getAuthorName(), user.getUsername(), reason)))
                                .onErrorResume(CommandException.class,
                                        err -> context.createFollowupMessage(Emoji.WARNING, err.getMessage())))))
                .then()
                .onErrorMap(ClientException.isStatusCode(HttpResponseStatus.FORBIDDEN.code()),
                        err -> new MissingPermissionException(this.permission));
    }

    public abstract Mono<?> action(Member memberToRemove, String reason);

    private Mono<Boolean> canInteract(Locale locale, Member self, Member author, Member memberToRemove) {
        if (self.equals(memberToRemove)) {
            return Mono.error(new CommandException(
                    I18nManager.localize(locale, "%s.exception.self".formatted(this.keyword))));
        }

        if (author.equals(memberToRemove)) {
            return Mono.error(new CommandException(
                    I18nManager.localize(locale, "%s.exception.author".formatted(this.keyword))));
        }

        return Mono.zip(self.isHigher(memberToRemove), author.isHigher(memberToRemove))
                .flatMap(TupleUtils.function((isSelfHigher, isAuthorHigher) -> {
                    if (!isSelfHigher) {
                        return Mono.error(
                                new CommandException(I18nManager.localize(locale, "%s.exception.self.higher".formatted(this.keyword))
                                        .formatted(memberToRemove.getUsername())));
                    }
                    if (!isAuthorHigher) {
                        return Mono.error(
                                new CommandException(I18nManager.localize(locale, "%s.exception.author.higher".formatted(this.keyword))
                                        .formatted(memberToRemove.getUsername())));
                    }
                    return Mono.just(true);
                }));
    }

    private Mono<Message> sendMessage(I18nContext context, Guild guild, Member author, Member memberToRemove, String reason) {
        if (memberToRemove.isBot()) {
            return Mono.empty();
        }
        return memberToRemove.getPrivateChannel()
                .flatMap(privateChannel -> DiscordUtil.sendMessage(Emoji.WARNING,
                        context.localize("%s.private.message".formatted(this.keyword))
                                .formatted(guild.getName(), author.getUsername(), reason), privateChannel))
                .switchIfEmpty(Mono.error(
                        new CommandException(context.localize("exception.private.message")
                                .formatted(memberToRemove.getUsername()))));
    }
}
