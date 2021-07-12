package com.shadorc.shadbot.command.info;

import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.command.GroupCmd;
import com.shadorc.shadbot.core.command.SubCmd;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import com.shadorc.shadbot.utils.TimeUtil;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.legacy.LegacyEmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.function.Consumer;

public class UserInfoCmd extends SubCmd {

    private final DateTimeFormatter dateFormatter;

    public UserInfoCmd(final GroupCmd groupCmd) {
        super(groupCmd, CommandCategory.INFO, "user", "Show user info");
        this.addOption("user", "If not specified, it will show your info", false,
                ApplicationCommandOptionType.USER);

        this.dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.MEDIUM);
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

    private Consumer<LegacyEmbedCreateSpec> formatEmbed(Context context, Member member, List<Role> roles) {
        final DateTimeFormatter dateFormatter = this.dateFormatter.withLocale(context.getLocale());

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
        final LocalDateTime createTime = TimeUtil.toLocalDateTime(member.getId().getTimestamp());
        final String creationField = "%s%n(%s)"
                .formatted(createTime.format(dateFormatter),
                        FormatUtil.formatLongDuration(context.getLocale(), createTime));

        final String joinTitle = Emoji.DATE + " " + context.localize("userinfo.join");
        final LocalDateTime joinTime = TimeUtil.toLocalDateTime(member.getJoinTime().orElseThrow());
        final String joinField = "%s%n(%s)"
                .formatted(joinTime.format(dateFormatter),
                        FormatUtil.formatLongDuration(context.getLocale(), joinTime));

        final String badgesField = FormatUtil.format(member.getPublicFlags(), FormatUtil::capitalizeEnum, "\n");
        final String rolesField = FormatUtil.format(roles, Role::getMention, "\n");

        return ShadbotUtil.getDefaultLegacyEmbed(
                embed -> {
                    embed.setAuthor(context.localize("userinfo.title").formatted(usernameBuilder), null, context.getAuthorAvatar())
                            .setThumbnail(member.getAvatarUrl())
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
                });
    }

}
