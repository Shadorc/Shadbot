package me.shadorc.shadbot.command.admin;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import discord4j.core.object.audit.AuditLogEntry;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.exception.MissingPermissionException.UserType;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Command(category = CommandCategory.ADMIN, permission = CommandPermission.ADMIN, names = { "kick" })
public class KickCmd extends AbstractCommand {

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
				.flatMapMany(channel -> DiscordUtils.requirePermissions(channel, context.getSelfId(), UserType.BOT, Permission.KICK_MEMBERS)
						.then(Mono.zip(context.getMessage().getUserMentions().collectList(), 
								context.getGuild(), 
								context.getSelfAsMember()))
						.flatMapMany(tuple -> {
							final List<User> mentions = tuple.getT1();
							final Guild guild = tuple.getT2();
							final Member self = tuple.getT3();

							final StringBuilder reason = new StringBuilder();
							reason.append(StringUtils.remove(arg, FormatUtils.format(mentions, User::getMention, " ")).trim());

							if(reason.length() == 0) {
								reason.append("Reason not specified.");
							}

							if(reason.length() > AuditLogEntry.MAX_REASON_LENGTH) {
								throw new CommandException(String.format("Reason cannot exceed **%d characters**.", AuditLogEntry.MAX_REASON_LENGTH));
							}

							return Flux.fromIterable(mentions)
									.filter(user -> !user.isBot())
									.flatMap(user -> user.asMember(context.getGuildId()))
									.concatMap(member -> member.getPrivateChannel()
											.cast(MessageChannel.class)
											.filterWhen(ignored -> DiscordUtils.isUserHigher(guild, self, member))
											.switchIfEmpty(Mono.error(new CommandException(
													String.format("I cannot kick **%s** because he is higher in the role hierarchy than me.", member.getUsername()))))
											.filterWhen(ignored -> DiscordUtils.isUserHigher(guild, context.getMember(), member))
											.switchIfEmpty(Mono.error(new CommandException(
													String.format("You cannot kick **%s** because he is higher in the role hierarchy than you.", member.getUsername()))))
											.flatMap(privateChannel -> DiscordUtils.sendMessage(
													String.format(Emoji.INFO + " You were kicked from the server **%s** by **%s**. Reason: `%s`",
															guild.getName(), context.getUsername(), reason), privateChannel))
											.then(member.kick(reason.toString()))
											.thenReturn(member));
						})
						.collectList()
						.flatMap(members -> DiscordUtils.sendMessage(
								String.format(Emoji.INFO + " **%s** kicked %s.",
										context.getUsername(),
										FormatUtils.format(members, member -> String.format("**%s**", member.getUsername()), ", ")),
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
