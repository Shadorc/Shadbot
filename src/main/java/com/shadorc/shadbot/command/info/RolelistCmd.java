package com.shadorc.shadbot.command.info;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Snowflake;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RolelistCmd extends BaseCmd {

    public RolelistCmd() {
        super(CommandCategory.INFO, List.of("rolelist"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        return context.getGuild()
                .flatMapMany(guild -> DiscordUtils.extractRoles(guild, arg))
                .collectList()
                .flatMap(mentionedRoles -> {
                    if (mentionedRoles.isEmpty()) {
                        return Mono.error(new CommandException(String.format("Role `%s` not found.", arg)));
                    }

                    final List<Snowflake> mentionedRoleIds = mentionedRoles
                            .stream()
                            .map(Role::getId)
                            .collect(Collectors.toList());

                    final Mono<List<String>> usernames = context.getGuild()
                            .flatMapMany(Guild::getMembers)
                            .filter(member -> !Collections.disjoint(member.getRoleIds(), mentionedRoleIds))
                            .map(Member::getUsername)
                            .distinct()
                            .collectList();

                    return Mono.zip(Mono.just(mentionedRoles), usernames);
                })
                .map(tuple -> DiscordUtils.getDefaultEmbed()
                        .andThen(embed -> {
                            embed.setAuthor(
                                    String.format("Rolelist: %s", FormatUtils.format(tuple.getT1(), Role::getName, ", ")),
                                    null, context.getAvatarUrl());

                            if (tuple.getT2().isEmpty()) {
                                embed.setDescription(
                                        String.format("There is nobody with %s.", tuple.getT1().size() == 1 ? "this role" : "these roles"));
                                return;
                            }

                            FormatUtils.createColumns(tuple.getT2(), 25)
                                    .forEach(field -> embed.addField(field.name(), field.value(), true));
                        }))
                .flatMap(embedConsumer -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(embedConsumer, channel)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Show a list of members with specific role(s).")
                .addArg("@role(s)", false)
                .build();
    }

}
