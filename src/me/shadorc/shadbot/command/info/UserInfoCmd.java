package me.shadorc.shadbot.command.info;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.util.Image.Format;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.INFO, names = { "userinfo", "user_info", "user-info" })
public class UserInfoCmd extends AbstractCommand {

	private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM uuuu - HH'h'mm", Locale.ENGLISH);

	@Override
	public Mono<Void> execute(Context context) {

		final Member member = context.getMember();

		return Mono.zip(context.getMessage().getUserMentions().switchIfEmpty(context.getAuthor()).single(),
				member.getPresence(),
				member.getRoles().collectList(),
				context.getAvatarUrl())
				.map(tuple4 -> {
					final User user = tuple4.getT1();
					final Presence presence = tuple4.getT2();
					final List<Role> roles = tuple4.getT3();
					final String avatarUrl = tuple4.getT4();

					final String creationDate = String.format("%s%n(%s)",
							TimeUtils.toLocalDate(DiscordUtils.getSnowflakeTimeFromID(user.getId())).format(dateFormatter),
							FormatUtils.formatLongDuration(DiscordUtils.getSnowflakeTimeFromID(user.getId())));

					final String joinDate = String.format("%s%n(%s)",
							TimeUtils.toLocalDate(member.getJoinTime()).format(dateFormatter),
							FormatUtils.formatLongDuration(member.getJoinTime()));

					EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed()
							.setAuthor(String.format("Info about user \"%s\"%s", user.getUsername(), user.isBot() ? " (Bot)" : ""), null, avatarUrl)
							.setThumbnail(user.getAvatar(Format.JPEG).get().getUrl())
							.addField("Display name", member.getDisplayName(), true)
							.addField("User ID", user.getId().asString(), true)
							.addField("Creation date", creationDate, true)
							.addField("Join date", joinDate, true);

					if(!roles.isEmpty()) {
						embed.addField("Roles", FormatUtils.format(roles, Role::getName, "\n"), true);
					}

					embed.addField("Status", StringUtils.capitalizeFully(presence.getStatus().getValue()), true);
					if(presence.getActivity().flatMap(Activity::getDetails).isPresent()) {
						embed.addField("Playing text", presence.getActivity().flatMap(Activity::getDetails).get(), true);
					}

					return embed;
				})
				.flatMap(embed -> BotUtils.sendMessage(embed, context.getChannel()))
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
