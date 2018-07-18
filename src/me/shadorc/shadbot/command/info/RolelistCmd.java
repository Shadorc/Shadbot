package me.shadorc.shadbot.command.info;

import java.util.Collections;
import java.util.Set;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.INFO, names = { "rolelist" })
public class RolelistCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		Set<Snowflake> roleIds = context.getMessage().getRoleMentionIds();
		if(roleIds.isEmpty()) {
			throw new MissingArgumentException();
		}

		final Flux<Role> roles = Flux.fromIterable(roleIds)
				.flatMap(roleId -> context.getClient().getRoleById(context.getGuildId(), roleId));

		EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed()
				.setAuthor("Role List", null, null);

		return context.getGuild()
				.flatMapMany(Guild::getMembers)
				.filter(member -> !Collections.disjoint(member.getRoleIds(), roleIds))
				.map(User::getUsername)
				.buffer()
				.zipWith(roles.buffer())
				.map(membersAndRoles -> {
					FormatUtils.createColumns(membersAndRoles.getT1(), 25).stream()
							.forEach(field -> embed.addField(field.getName(), field.getValue(), false));

					return embed.setDescription(String.format("Members with role(s) **%s**",
							FormatUtils.format(membersAndRoles.getT2(), Role::getName, ", ")));
				})
				.defaultIfEmpty(embed.setDescription(String.format("There is nobody with %s.", roleIds.size() == 1 ? "this role" : "these roles")))
				.then(BotUtils.sendMessage(embed, context.getChannel()))
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show a list of members with specific role(s).")
				.addArg("@role(s)", false)
				.build();
	}

}
