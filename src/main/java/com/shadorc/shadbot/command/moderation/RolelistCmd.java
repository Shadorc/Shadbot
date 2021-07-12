package com.shadorc.shadbot.command.moderation;

import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.command.GroupCmd;
import com.shadorc.shadbot.core.command.SubCmd;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RolelistCmd extends SubCmd {

    @Override
    public Mono<?> execute(Context context) {
        final Flux<Role> getRoles = Flux.fromStream(IntStream.range(1, 4).boxed())
                .flatMap(index -> context.getOptionAsRole("role%d".formatted(index)));

        return getRoles
                .collectList()
                .flatMap(mentionedRoles -> {
                    final List<Snowflake> mentionedRoleIds = mentionedRoles
                            .stream()
                            .map(Role::getId)
                            .collect(Collectors.toList());

                    final Mono<List<String>> getUsernames = context.getGuild()
                            .flatMapMany(Guild::requestMembers)
                            .filter(member -> !Collections.disjoint(member.getRoleIds(), mentionedRoleIds))
                            .map(Member::getUsername)
                            .distinct()
                            .collectList();

                    return Mono.zip(Mono.just(mentionedRoles), getUsernames);
                })
                .map(TupleUtils.function((mentionedRoles, usernames) -> {
                    final EmbedCreateSpec.Builder embed = ShadbotUtil.createEmbedBuilder()
                            .author(context.localize("rolelist.title"), null, context.getAuthorAvatar());

                    if (usernames.isEmpty()) {
                        if (mentionedRoles.size() == 1) {
                            embed.description(context.localize("rolelist.nobody.singular"));
                        } else {
                            embed.description(context.localize("rolelist.nobody.plural"));
                        }
                        return embed.build();
                    }

                    final String rolesFormatted = FormatUtil.format(mentionedRoles, Role::getMention, ", ");
                    embed.description(context.localize("rolelist.description")
                            .formatted(rolesFormatted));

                    FormatUtil.createColumns(usernames, 25)
                            .forEach(field -> embed.addField(field.name(), field.value(), true));

                    return embed.build();
                }))
                .flatMap(context::createFollowupMessage);
    }

    public RolelistCmd(final GroupCmd groupCmd) {
        super(groupCmd, CommandCategory.MODERATION, "rolelist", "Show a list of members with specific role(s)");

        this.addOption(option -> option.name("role1")
                .description("The first role to have")
                .required(true)
                .type(ApplicationCommandOptionType.ROLE.getValue()));
        this.addOption(option -> option.name("role2")
                .description("The second role to have")
                .required(false)
                .type(ApplicationCommandOptionType.ROLE.getValue()));
        this.addOption(option -> option.name("role3")
                .description("The third role to have")
                .required(false)
                .type(ApplicationCommandOptionType.ROLE.getValue()));
    }

}
