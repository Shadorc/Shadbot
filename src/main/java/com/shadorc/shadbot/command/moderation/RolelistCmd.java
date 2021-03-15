package com.shadorc.shadbot.command.moderation;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RolelistCmd extends BaseCmd {

    // TODO: Uses varargs when available
    public RolelistCmd() {
        super(CommandCategory.MODERATION, "rolelist", "Show a list of members with specific role(s)");
        this.addOption("role_1", "The first role to have", true, ApplicationCommandOptionType.ROLE);
        this.addOption("role_2", "The second role to have", false, ApplicationCommandOptionType.ROLE);
        this.addOption("role_3", "The third role to have", false, ApplicationCommandOptionType.ROLE);
    }

    @Override
    public Mono<?> execute(Context context) {
        return Flux.merge(
                context.getOptionAsRole("role_1"),
                context.getOptionAsRole("role_2"),
                context.getOptionAsRole("role_3"))
                .collectList()
                .flatMap(mentionedRoles -> {
                    final List<Snowflake> mentionedRoleIds = mentionedRoles
                            .stream()
                            .map(Role::getId)
                            .collect(Collectors.toList());

                    final Mono<List<String>> getUsernames = context.getGuild()
                            .flatMapMany(Guild::getMembers)
                            .filter(member -> !Collections.disjoint(member.getRoleIds(), mentionedRoleIds))
                            .map(Member::getUsername)
                            .distinct()
                            .collectList();

                    return Mono.zip(Mono.just(mentionedRoles), getUsernames);
                })
                .map(TupleUtils.function((mentionedRoles, usernames) -> ShadbotUtil.getDefaultEmbed(
                        embed -> {
                            final String rolesFormatted = FormatUtil.format(mentionedRoles, Role::getName, ", ");
                            embed.setAuthor(String.format("Rolelist: %s", rolesFormatted), null, context.getAuthorAvatar());

                            if (usernames.isEmpty()) {
                                embed.setDescription("There is nobody with %s."
                                        .formatted(mentionedRoles.size() == 1 ? "this role" : "these roles"));
                                return;
                            }

                            FormatUtil.createColumns(usernames, 25)
                                    .forEach(field -> embed.addField(field.name(), field.value(), true));
                        })))
                .flatMap(context::createFollowupMessage);
    }

}
