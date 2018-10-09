package me.shadorc.shadbot.command.admin;

import java.util.List;
import java.util.Set;

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
import me.shadorc.shadbot.exception.MissingPermissionException.Type;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
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
			throw new CommandException("I can't (and don't want) to kick myself !");
		}

		return DiscordUtils.requirePermissions(context.getChannel(), context.getAuthorId(), Type.USER, Permission.KICK_MEMBERS)
				.then(DiscordUtils.requirePermissions(context.getChannel(), context.getSelfId(), Type.BOT, Permission.KICK_MEMBERS))
				.then(Mono.zip(context.getMessage().getUserMentions().collectList(), context.getGuild()))
				.flatMap(tuple -> {
					final List<User> mentions = tuple.getT1();
					final Guild guild = tuple.getT2();

					final StringBuilder reason = new StringBuilder();
					reason.append(StringUtils.remove(arg, FormatUtils.format(mentions, User::getMention, " ")).trim());
					if(reason.length() > DiscordUtils.MAX_REASON_LENGTH) {
						throw new CommandException(String.format("Reason cannot exceed **%d characters**.", DiscordUtils.MAX_REASON_LENGTH));
					}

					if(reason.length() == 0) {
						reason.append("Reason not specified.");
					}

					Flux<Void> kickFlux = Flux.empty();
					for(User user : mentions) {
						if(!user.isBot()) {
							kickFlux = kickFlux.concatWith(BotUtils.sendMessage(
									String.format(Emoji.INFO + " You were kicked from the server **%s** by **%s**. Reason: `%s`",
											guild.getName(), context.getUsername(), reason), user.getPrivateChannel().cast(MessageChannel.class))
									.then());
						}

						// TODO: Add reason
						kickFlux = kickFlux.concatWith(user.asMember(context.getGuildId())
								.flatMap(Member::kick));
					}

					return kickFlux
							.then(BotUtils.sendMessage(String.format(Emoji.INFO + " **%s** got kicked by **%s**. Reason: `%s`",
									FormatUtils.format(mentions, User::getUsername, ", "), context.getUsername(), reason),
									context.getChannel()))
							.then();
				});
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Kick user(s).")
				.addArg("@user(s)", false)
				.addArg("reason", true)
				.build();
	}

}
