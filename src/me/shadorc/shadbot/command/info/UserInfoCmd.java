package me.shadorc.shadbot.command.info;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

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
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.INFO, names = { "userinfo", "user_info", "user-info" })
public class UserInfoCmd extends AbstractCommand {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM uuuu - HH'h'mm", Locale.ENGLISH);

	@Override
	public Mono<Void> execute(Context context) {
		final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		final Mono<Member> memberMono = context.getMessage()
				.getUserMentions()
				.switchIfEmpty(Mono.just(context.getAuthor()))
				.next()
				.flatMap(user -> user.asMember(context.getGuildId()));

		return Mono.zip(memberMono,
				memberMono.flatMap(Member::getPresence),
				memberMono.flatMapMany(Member::getRoles).collectList())
				.map(tuple4 -> {
					final Member member = tuple4.getT1();
					final Presence presence = tuple4.getT2();
					final List<Role> roles = tuple4.getT3();

					final String creationDate = String.format("%s%n(%s)",
							TimeUtils.toLocalDate(member.getId().getTimestamp()).format(DATE_FORMATTER),
							FormatUtils.longDuration(member.getId().getTimestamp()));

					final String joinDate = String.format("%s%n(%s)",
							TimeUtils.toLocalDate(member.getJoinTime()).format(DATE_FORMATTER),
							FormatUtils.longDuration(member.getJoinTime()));

					return EmbedUtils.getDefaultEmbed()
							.andThen(embed -> {
								embed.setAuthor(String.format("User Info: %s%s", member.getUsername(), member.isBot() ? " (Bot)" : ""), null, context.getAvatarUrl())
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
							});
				})
				.flatMap(loadingMsg::send)
				.then();
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show info about an user.")
				.addArg("@user", true)
				.build();
	}

}
