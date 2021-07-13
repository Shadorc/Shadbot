package com.shadorc.shadbot.command.info;

import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.command.GroupCmd;
import com.shadorc.shadbot.core.command.SubCmd;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.common.util.TimestampFormat;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.Instant;
import java.util.List;

public class UserInfoCmd extends SubCmd {

    public UserInfoCmd(final GroupCmd groupCmd) {
        super(groupCmd, CommandCategory.INFO, "user", "Show user info");
        this.addOption(option -> option.name("user")
                .description("If not specified, it will show your info")
                .required(false)
                .type(ApplicationCommandOptionType.USER.getValue()));
    }

    @Override
    public Mono<?> execute(Context context) {
        final Mono<Member> getUser = context.getOptionAsMember("user")
                .defaultIfEmpty(context.getAuthor())
                .cache();

        return Mono.zip(getUser, getUser.flatMapMany(Member::getRoles).collectList())
                .map(TupleUtils.function((user, roles) -> this.formatEmbed(context, user, roles)))
                .flatMap(context::createFollowupMessage);
    }

    private EmbedCreateSpec formatEmbed(Context context, Member member, List<Role> roles) {
        final StringBuilder usernameBuilder = new StringBuilder(member.getTag());
        if (member.isBot()) {
            usernameBuilder
                    .append(" ")
                    .append(context.localize("userinfo.bot"));
        }
        if (member.getPremiumTime().isPresent()) {
            usernameBuilder
                    .append(" ")
                    .append(context.localize("userinfo.booster"));
        }

        final String idTitle = Emoji.ID + " " + context.localize("userinfo.id");
        final String nameTitle = Emoji.BUST_IN_SILHOUETTE + " " + context.localize("userinfo.name");

        final String creationTitle = Emoji.BIRTHDAY + " " + context.localize("userinfo.creation");
        final Instant creationInstant = member.getId().getTimestamp();
        final String creationField = "%s%n(%s)"
                .formatted(TimestampFormat.SHORT_DATE_TIME.format(creationInstant),
                        FormatUtil.formatRelativeTime(context.getLocale(), creationInstant));

        final String joinTitle = Emoji.DATE + " " + context.localize("userinfo.join");
        final Instant joinInstant = member.getJoinTime().orElseThrow();
        final String joinField = "%s%n(%s)"
                .formatted(TimestampFormat.SHORT_DATE_TIME.format(joinInstant),
                        FormatUtil.formatRelativeTime(context.getLocale(), joinInstant));

        final String badgesField = FormatUtil.format(member.getPublicFlags(), FormatUtil::capitalizeEnum, "\n");
        final String rolesField = FormatUtil.format(roles, Role::getMention, "\n");

        final EmbedCreateSpec.Builder embed = ShadbotUtil.createEmbedBuilder()
                .author(context.localize("userinfo.title").formatted(usernameBuilder), null, context.getAuthorAvatar())
                .thumbnail(member.getAvatarUrl())
                .addField(idTitle, member.getId().asString(), true)
                .addField(nameTitle, member.getDisplayName(), true)
                .addField(creationTitle, creationField, true)
                .addField(joinTitle, joinField, true);

        if (!badgesField.isEmpty()) {
            final String badgesTitle = Emoji.MILITARY_MEDAL + " " + context.localize("userinfo.badges");
            embed.addField(badgesTitle, badgesField, true);
        }

        if (!rolesField.isEmpty()) {
            final String rolesTitle = Emoji.LOCK + " " + context.localize("userinfo.roles");
            embed.addField(rolesTitle, rolesField, true);
        }

        return embed.build();
    }

}
