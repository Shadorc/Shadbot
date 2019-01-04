package me.shadorc.shadbot.command.info;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.INFO, names = { "userinfo", "user_info", "user-info" })
public class UserInfoCmd extends AbstractCommand {

	private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM uuuu - HH'h'mm", Locale.ENGLISH);

	@Override
	public Mono<Void> execute(Context context) {

		final Mono<Member> memberMono = context.getMessage()
				.getUserMentions()
				.switchIfEmpty(context.getAuthor())
				.next()
				.flatMap(user -> user.asMember(context.getGuildId()));

		return Mono.zip(memberMono,
				memberMono.flatMap(Member::getPresence),
				memberMono.flatMapMany(Member::getRoles).collectList(),
				context.getAvatarUrl())
				.map(tuple4 -> {
					final Member member = tuple4.getT1();
					final Presence presence = tuple4.getT2();
					final List<Role> roles = tuple4.getT3();
					final String avatarUrl = tuple4.getT4();

					final String creationDate = String.format("%s%n(%s)",
							TimeUtils.toLocalDate(member.getId().getTimestamp()).format(this.dateFormatter),
							FormatUtils.longDuration(member.getId().getTimestamp()));

					final String joinDate = String.format("%s%n(%s)",
							TimeUtils.toLocalDate(member.getJoinTime()).format(this.dateFormatter),
							FormatUtils.longDuration(member.getJoinTime()));

					final EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed()
							.setAuthor(String.format("User Info: %s%s", member.getUsername(), member.isBot() ? " (Bot)" : ""), null, avatarUrl)
							.setThumbnail(member.getAvatarUrl())
							.addField("Display name", member.getDisplayName(), true)
							.addField("User ID", member.getId().asString(), true)
							.addField("Creation date", creationDate, true)
							.addField("Join date", joinDate, true);

					if(!roles.isEmpty()) {
						embed.addField("Roles", FormatUtils.format(roles, Role::getMention, "\n"), true);
					}

					embed.addField("Status", StringUtils.capitalize(presence.getStatus().getValue()), true);
					presence.getActivity()
							.map(Activity::getName)
							.ifPresent(details -> embed.addField("Playing text", details, true));

					return embed;
				})
				.flatMap(embed -> context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(embed, channel)))
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show info about an user.")
				.addArg("@user", true)
				.build();
	}

}
