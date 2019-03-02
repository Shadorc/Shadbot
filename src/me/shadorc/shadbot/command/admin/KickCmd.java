package me.shadorc.shadbot.command.admin;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import discord4j.core.object.audit.AuditLogEntry;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.ratelimiter.RateLimiter;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.exception.ExceptionUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class KickCmd extends BaseCmd {

	public KickCmd() {
		super(CommandCategory.ADMIN, CommandPermission.ADMIN, List.of("kick"));
		this.setRateLimite(new RateLimiter(2, Duration.ofSeconds(3)));
	}

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		final Set<Snowflake> mentionedUserIds = context.getMessage().getUserMentionIds();
		if(mentionedUserIds.isEmpty()) {
			throw new MissingArgumentException();
		}

		if(mentionedUserIds.contains(context.getAuthorId())) {
			throw new CommandException("You cannot kick yourself.");
		}

		if(mentionedUserIds.contains(context.getSelfId())) {
			throw new CommandException("You cannot kick me.");
		}

		return context.getChannel()
				.flatMapMany(channel -> DiscordUtils.requirePermissions(channel, Permission.KICK_MEMBERS)
						.then(Mono.zip(context.getMessage().getUserMentions().collectList(),
								context.getGuild(),
								context.getSelfAsMember()))
						.flatMapMany(tuple -> {
							final List<User> mentionedUsers = tuple.getT1();
							final Guild guild = tuple.getT2();
							final Member self = tuple.getT3();

							final StringBuilder reason = new StringBuilder();
							final List<String> mentions = mentionedUsers.stream()
									.map(User::getMention)
									.collect(Collectors.toList());
							reason.append(StringUtils.remove(arg, mentions).trim());

							if(reason.length() == 0) {
								reason.append("Reason not specified.");
							}

							if(reason.length() > AuditLogEntry.MAX_REASON_LENGTH) {
								return Flux.error(new CommandException(
										String.format("Reason cannot exceed **%d characters**.", AuditLogEntry.MAX_REASON_LENGTH)));
							}

							return Flux.fromIterable(mentionedUsers)
									.filter(user -> !user.isBot())
									.flatMap(user -> user.asMember(context.getGuildId()))
									.concatMap(member -> member.getPrivateChannel()
											.cast(MessageChannel.class)
											.filterWhen(ignored -> DiscordUtils.isUserHigher(guild, self, member))
											.switchIfEmpty(DiscordUtils.sendMessage(
													String.format(Emoji.WARNING + " (**%s**) I cannot kick **%s** because he is higher in the role hierarchy than me.",
															context.getUsername(), member.getUsername()), channel)
													.then(Mono.empty()))
											.filterWhen(ignored -> DiscordUtils.isUserHigher(guild, context.getMember(), member))
											.switchIfEmpty(DiscordUtils.sendMessage(
													String.format(Emoji.WARNING + " (**%s**) You cannot kick **%s** because he is higher in the role hierarchy than you.",
															context.getUsername(), member.getUsername()), channel)
													.then(Mono.empty()))
											.flatMap(privateChannel -> DiscordUtils.sendMessage(
													String.format(Emoji.INFO + " You were kicked from the server **%s** by **%s**. Reason: `%s`",
															guild.getName(), context.getUsername(), reason), privateChannel))
											.onErrorResume(ExceptionUtils::isDiscordForbidden, err -> DiscordUtils.sendMessage(
													String.format(Emoji.WARNING + " (**%s**) I could not send a message to **%s**.",
															context.getUsername(), member.getUsername()), channel))
											.then(member.kick(reason.toString()))
											.thenReturn(member));
						})
						.map(Member::getUsername)
						.collectList()
						.flatMap(memberUsernames -> DiscordUtils.sendMessage(
								String.format(Emoji.INFO + " **%s** kicked %s.",
										context.getUsername(),
										FormatUtils.format(memberUsernames, username -> String.format("**%s**", username), ", ")),
								channel)))
				.then();
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Kick user(s).")
				.addArg("@user(s)", false)
				.addArg("reason", true)
				.build();
	}

}
