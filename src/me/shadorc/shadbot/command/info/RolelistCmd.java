package me.shadorc.shadbot.command.info;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
				.flatMap(mentionedRoleIds -> {
					if(mentionedRoleIds.isEmpty()) {
						throw new CommandException(String.format("Role `%s` not found.", arg));
					}

					final Mono<List<Role>> mentionedRoles = Flux.fromIterable(mentionedRoleIds)
							.flatMap(roleId -> context.getClient().getRoleById(context.getGuildId(), roleId))
							.collectList();

					final Mono<List<String>> usernames = context.getGuild()
							.flatMapMany(Guild::getMembers)
							.filter(member -> !Collections.disjoint(member.getRoleIds(), mentionedRoleIds))
							.map(Member::getUsername)
							.distinct()
							.collectList();

					return Mono.zip(mentionedRoles, usernames);
				})
				.map(tuple -> EmbedUtils.getDefaultEmbed()
						.andThen(embed -> {
							embed.setAuthor(String.format("Rolelist: %s", FormatUtils.format(tuple.getT1(), Role::getName, ", ")),
									null, context.getAvatarUrl());

							if(tuple.getT2().isEmpty()) {
								embed.setDescription(
										String.format("There is nobody with %s.", tuple.getT1().size() == 1 ? "this role" : "these roles"));
								return;
							}

							FormatUtils.createColumns(tuple.getT2(), 25).stream()
									.forEach(field -> embed.addField(field.getName(), field.getValue(), true));
						}))
				.flatMap(embedConsumer -> context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(embedConsumer, channel)))
				.then();
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show a list of members with specific role(s).")
				.addArg("@role(s)", false)
				.build();
	}

}
