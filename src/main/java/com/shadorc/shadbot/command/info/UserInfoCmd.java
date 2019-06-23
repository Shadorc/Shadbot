package com.shadorc.shadbot.command.info;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.StringUtils;
import com.shadorc.shadbot.utils.TimeUtils;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class UserInfoCmd extends BaseCmd {

    private final DateTimeFormatter dateFormatter;

    public UserInfoCmd() {
        super(CommandCategory.INFO, List.of("user_info", "user-info", "userinfo"));
        this.setDefaultRateLimiter();

        this.dateFormatter = DateTimeFormatter.ofPattern("d MMMM uuuu - HH'h'mm", Locale.ENGLISH);
    }

    @Override
    public Mono<Void> execute(Context context) {
        final Mono<Member> getMember = context.getMessage()
                .getUserMentions()
                .switchIfEmpty(Mono.just(context.getAuthor()))
                .next()
                .flatMap(user -> user.asMember(context.getGuildId()));

        return Mono.zip(getMember,
                getMember.flatMap(Member::getPresence),
                getMember.flatMapMany(Member::getRoles).collectList())
                .map(tuple -> this.getEmbed(tuple.getT1(), tuple.getT2(), tuple.getT3(), context.getAvatarUrl()))
                .flatMap(embed -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(embed, channel)))
                .then();
    }

    private Consumer<EmbedCreateSpec> getEmbed(Member member, Presence presence, List<Role> roles, String avatarUrl) {
        final String creationDate = String.format("%s%n(%s)",
                TimeUtils.toLocalDate(member.getId().getTimestamp()).format(this.dateFormatter),
                FormatUtils.longDuration(member.getId().getTimestamp()));

        final String joinDate = String.format("%s%n(%s)",
                TimeUtils.toLocalDate(member.getJoinTime()).format(this.dateFormatter),
                FormatUtils.longDuration(member.getJoinTime()));

        return DiscordUtils.getDefaultEmbed()
                .andThen(embed -> {
                    embed.setAuthor(String.format("User Info: %s%s", member.getUsername(), member.isBot() ? " (Bot)" : ""), null, avatarUrl)
                            .setThumbnail(member.getAvatarUrl())
                            .addField("Display name", member.getDisplayName(), true)
                            .addField("User ID", member.getId().asString(), true)
                            .addField("Creation date", creationDate, true)
                            .addField("Join date", joinDate, true);

                    if (!roles.isEmpty()) {
                        embed.addField("Roles", FormatUtils.format(roles, Role::getMention, "\n"), true);
                    }

                    embed.addField("Status", StringUtils.capitalize(presence.getStatus().getValue()), true);
                    presence.getActivity()
                            .map(Activity::getName)
                            .ifPresent(details -> embed.addField("Playing text", details, true));
                });
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show info about an user.")
                .addArg("@user", true)
                .build();
    }

}
