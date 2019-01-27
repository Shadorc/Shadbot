package me.shadorc.shadbot.command.info;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
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
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.INFO, names = { "rolelist" })
public class RolelistCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		return context.getGuild()
				.flatMapMany(guild -> DiscordUtils.extractRoles(guild, arg))
				.flatMap(roleId -> context.getClient().getRoleById(context.getGuildId(), roleId))
				.collectList()
				.flatMap(mentionedRoles -> {
					if(mentionedRoles.isEmpty()) {
						throw new CommandException(String.format("Role `%s` not found.", arg));
					}

					final List<Snowflake> mentionedRoleIds = mentionedRoles.stream()
							.map(Role::getId)
							.collect(Collectors.toList());
					
					final Mono<List<String>> usernameList = context.getGuild()
							.flatMapMany(Guild::getMembers)
							.filter(member -> !Collections.disjoint(member.getRoleIds(), mentionedRoleIds))
							.map(Member::getUsername)
							.distinct()
							.collectList();
					
					return Mono.zip(usernameList, context.getAvatarUrl())
							.map(tuple -> {
								final String avatarUrl = tuple.getT2();
								final List<String> usernames = tuple.getT1();
								
								final Consumer<? super EmbedCreateSpec> embedConsumer = embed -> {
									EmbedUtils.getDefaultEmbed().accept(embed);
									embed.setAuthor(String.format("Rolelist: %s", FormatUtils.format(mentionedRoles, Role::getName, ", ")), 
											null, avatarUrl);
									
									if(usernames.isEmpty()) {
										embed.setDescription(
												String.format("There is nobody with %s.", mentionedRoleIds.size() == 1 ? "this role" : "these roles"));
										return;
									}
									
									FormatUtils.createColumns(usernames, 25).stream()
										.forEach(field -> embed.addField(field.getName(), field.getValue(), true));
								};
								
								return embedConsumer;
							})
							.flatMap(embedConsumer -> context.getChannel()
									.flatMap(channel -> DiscordUtils.sendMessage(embedConsumer, channel)));
				})
				.then();
	}

	@Override
	public Mono<Consumer<? super EmbedCreateSpec>> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show a list of members with specific role(s).")
				.addArg("@role(s)", false)
				.build();
	}

}
