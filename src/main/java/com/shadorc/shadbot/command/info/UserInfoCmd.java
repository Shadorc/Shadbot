package com.shadorc.shadbot.command.info;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import com.shadorc.shadbot.utils.TimeUtil;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class UserInfoCmd extends BaseCmd {

    private final DateTimeFormatter dateFormatter;

    public UserInfoCmd() {
        super(CommandCategory.INFO, "user_info", "Show info about a user");
        this.setDefaultRateLimiter();

        this.dateFormatter = DateTimeFormatter.ofPattern("d MMMM uuuu - HH'h'mm", Locale.ENGLISH);
    }

    @Override
    public ApplicationCommandRequest build(ImmutableApplicationCommandRequest.Builder builder) {
        return builder
                .addOption(ApplicationCommandOptionData.builder()
                        .name("user")
                        .description("If not specified, it will show your info")
                        .type(ApplicationCommandOptionType.USER.getValue())
                        .required(false)
                        .build())
                .build();
    }

    @Override
    public Mono<?> execute(Context context) {
        final Mono<Member> getMemberOrAuthor = context.getOptionAsMember("user")
                .defaultIfEmpty(context.getAuthor())
                .cache();

        return Mono.zip(getMemberOrAuthor, getMemberOrAuthor.flatMapMany(Member::getRoles).collectList())
                .map(TupleUtils.function((user, roles) -> this.getEmbed(user, roles, context.getAuthorAvatarUrl())))
                .flatMap(context::createFollowupMessage);
    }

    private Consumer<EmbedCreateSpec> getEmbed(Member member, List<Role> roles, String avatarUrl) {
        final LocalDateTime createTime = TimeUtil.toLocalDateTime(member.getId().getTimestamp());
        final String creationDate = String.format("%s%n(%s)",
                createTime.format(this.dateFormatter), FormatUtil.formatLongDuration(createTime));

        final LocalDateTime joinTime = TimeUtil.toLocalDateTime(member.getJoinTime());
        final String joinDate = String.format("%s%n(%s)",
                joinTime.format(this.dateFormatter), FormatUtil.formatLongDuration(joinTime));

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
                            .addField("User ID", member.getId().asString(), true)
                            .addField("Display name", member.getDisplayName(), true)
                            .addField("Creation date", creationDate, false)
                            .addField("Join date", joinDate, false);

                    if (!roles.isEmpty()) {
                        embed.addField("Roles", FormatUtil.format(roles, Role::getMention, "\n"), false);
                    }
                });
    }

}
