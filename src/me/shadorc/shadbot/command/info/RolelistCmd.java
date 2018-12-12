package me.shadorc.shadbot.command.info;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
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
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.INFO, names = { "rolelist" })
public class RolelistCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final Set<Snowflake> roleIds = context.getMessage().getRoleMentionIds();
		if(roleIds.isEmpty()) {
			throw new MissingArgumentException();
		}

		return context.getGuild()
				.flatMapMany(Guild::getMembers)
				.filter(member -> !Collections.disjoint(member.getRoleIds(), roleIds))
				.map(Member::getUsername)
				.collectList()
				.zipWith(context.getMessage().getRoleMentions().collectList())
				.map(usernamesAndRoles -> {
					final List<String> usernames = usernamesAndRoles.getT1().stream().distinct().collect(Collectors.toList());
					final List<Role> roles = usernamesAndRoles.getT2();

					final EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed();

					if(usernames.isEmpty()) {
						return embed.setDescription(
								String.format("There is nobody with %s.", roleIds.size() == 1 ? "this role" : "these roles"));
					}

					FormatUtils.createColumns(usernames, 25).stream()
							.forEach(field -> embed.addField(field.getName(), field.getValue(), true));

					return embed.setDescription(String.format("Members with role(s): **%s**",
							FormatUtils.format(roles, Role::getName, ", ")));
				})
				.zipWith(context.getAvatarUrl())
				.map(embedAndAvatarUrl -> embedAndAvatarUrl.getT1().setAuthor("Rolelist", null, embedAndAvatarUrl.getT2()))
				.flatMap(embed -> BotUtils.sendMessage(embed, context.getChannel()))
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
