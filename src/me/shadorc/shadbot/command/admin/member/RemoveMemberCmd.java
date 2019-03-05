package me.shadorc.shadbot.command.admin.member;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import discord4j.core.object.audit.AuditLogEntry;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
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
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public abstract class RemoveMemberCmd extends BaseCmd {

	private final String conjugatedVerb;
	private final Permission permission;

	public RemoveMemberCmd(String name, String conjugatedVerb, Permission permission) {
		super(CommandCategory.ADMIN, CommandPermission.ADMIN, List.of(name));
		this.setRateLimite(new RateLimiter(2, Duration.ofSeconds(3)));

		this.conjugatedVerb = conjugatedVerb;
		this.permission = permission;
	}

	public abstract Mono<Void> action(Member member, String reason);

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		final Set<Snowflake> mentionedUserIds = context.getMessage().getUserMentionIds();
		if(mentionedUserIds.isEmpty()) {
			throw new MissingArgumentException();
		}

		if(mentionedUserIds.contains(context.getAuthorId())) {
			throw new CommandException(String.format("You cannot %s yourself.", this.getName()));
		}

		if(mentionedUserIds.contains(context.getSelfId())) {
			throw new CommandException(String.format("You cannot %s me.", this.getName()));
		}

		return context.getChannel()
				.flatMapMany(channel -> DiscordUtils.requirePermissions(channel, this.permission)
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
									.filter(userToRemove -> !userToRemove.isBot())
									.flatMap(userToRemove -> userToRemove.asMember(context.getGuildId()))
									.concatMap(memberToRemove -> memberToRemove.getPrivateChannel()
											.cast(MessageChannel.class)
											.filterWhen(ignored -> self.isHigher(memberToRemove))
											.switchIfEmpty(DiscordUtils.sendMessage(
													String.format(Emoji.WARNING + " (**%s**) I cannot %s **%s** because he is higher in the role hierarchy than me.",
															context.getUsername(), this.getName(), memberToRemove.getUsername()), channel)
													.then(Mono.empty()))
											.filterWhen(ignored -> context.getMember().isHigher(memberToRemove))
											.switchIfEmpty(DiscordUtils.sendMessage(
													String.format(Emoji.WARNING + " (**%s**) You cannot %s **%s** because he is higher in the role hierarchy than you.",
															context.getUsername(), this.getName(), memberToRemove.getUsername()), channel)
													.then(Mono.empty()))
											.flatMap(privateChannel -> DiscordUtils.sendMessage(
													String.format(Emoji.INFO + " You were %s from the server **%s** by **%s**. Reason: `%s`",
															this.conjugatedVerb, guild.getName(), context.getUsername(), reason), privateChannel))
											.switchIfEmpty(DiscordUtils.sendMessage(
													String.format(Emoji.WARNING + " (**%s**) I could not send a message to **%s**.",
															context.getUsername(), memberToRemove.getUsername()), channel))
											.then(this.action(memberToRemove, reason.toString()))
											.thenReturn(memberToRemove));
						})
						.map(Member::getUsername)
						.collectList()
						.flatMap(memberUsernames -> DiscordUtils.sendMessage(
								String.format(Emoji.INFO + " **%s** %s %s.",
										context.getUsername(),
										this.conjugatedVerb,
										FormatUtils.format(memberUsernames, username -> String.format("**%s**", username), ", ")),
								channel)))
				.then();
	}

}
