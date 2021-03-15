package com.shadorc.shadbot.command.info;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import com.shadorc.shadbot.utils.TimeUtil;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

class UserInfoCmd extends BaseCmd {

    private final DateTimeFormatter dateFormatter;

    public UserInfoCmd() {
        super(CommandCategory.INFO, "user", "Show user info");
        this.addOption("user", "If not specified, it will show your info", false,
                ApplicationCommandOptionType.USER);

        this.dateFormatter = DateTimeFormatter.ofPattern("d MMMM uuuu - HH'h'mm", Config.DEFAULT_LOCALE);
    }

    @Override
    public Mono<?> execute(Context context) {
        final Mono<Member> getMemberOrAuthor = context.getOptionAsMember("user")
                .defaultIfEmpty(context.getAuthor())
                .cache();

        return Mono.zip(getMemberOrAuthor, getMemberOrAuthor.flatMapMany(Member::getRoles).collectList())
                .map(TupleUtils.function((user, roles) -> this.formatEmbed(user, roles, context.getAuthorAvatar())))
                .flatMap(context::createFollowupMessage);
    }

    private Consumer<EmbedCreateSpec> formatEmbed(Member member, List<Role> roles, String avatarUrl) {
        final LocalDateTime createTime = TimeUtil.toLocalDateTime(member.getId().getTimestamp());
        final String creationDate = String.format("%s%n(%s)",
                createTime.format(this.dateFormatter), FormatUtil.formatLongDuration(createTime));

        final LocalDateTime joinTime = TimeUtil.toLocalDateTime(member.getJoinTime());
        final String joinDate = String.format("%s%n(%s)",
                joinTime.format(this.dateFormatter), FormatUtil.formatLongDuration(joinTime));

        final String badges = FormatUtil.format(member.getPublicFlags(), FormatUtil::capitalizeEnum, "\n");
        final String rolesMention = FormatUtil.format(roles, Role::getMention, "\n");

        final StringBuilder usernameBuilder = new StringBuilder(member.getTag());
        if (member.isBot()) {
            usernameBuilder.append(" (Bot)");
        }
        if (member.getPremiumTime().isPresent()) {
            usernameBuilder.append(" (Booster)");
        }

        return ShadbotUtil.getDefaultEmbed(
                embed -> {
                    embed.setAuthor(String.format("User Info: %s", usernameBuilder), null, avatarUrl)
                            .setThumbnail(member.getAvatarUrl())
                            .addField(Emoji.ID + " User ID", member.getId().asString(), true)
                            .addField(Emoji.BUST_IN_SILHOUETTE + " Display name", member.getDisplayName(), true)
                            .addField(Emoji.BIRTHDAY + " Creation date", creationDate, true)
                            .addField(Emoji.DATE + " Join date", joinDate, true);

                    if (!badges.isEmpty()) {
                        embed.addField(Emoji.MILITARY_MEDAL + " Badges", badges, true);
                    }

                    if (!rolesMention.isEmpty()) {
                        embed.addField(Emoji.LOCK + " Roles", rolesMention, true);
                    }
                });
    }

}
