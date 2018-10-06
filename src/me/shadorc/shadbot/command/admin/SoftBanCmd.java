package me.shadorc.shadbot.command.admin;

import java.util.List;
import java.util.Set;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.BanQuerySpec;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.exception.MissingPermissionException;
import me.shadorc.shadbot.exception.MissingPermissionException.Type;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Command(category = CommandCategory.ADMIN, permission = CommandPermission.ADMIN, names = { "softban" })
public class SoftBanCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		final Set<Snowflake> mentionedUserIds = context.getMessage().getUserMentionIds();
		if(mentionedUserIds.isEmpty()) {
			throw new MissingArgumentException();
		}

		if(mentionedUserIds.contains(context.getAuthorId())) {
			throw new CommandException("You cannot softban yourself.");
		}

		final Snowflake guildId = context.getGuildId();

		return Mono.zip(context.getMessage().getUserMentions().collectList(), 
				context.getGuild(), 
				DiscordUtils.hasPermission(context.getChannel(), context.getAuthorId(), Permission.BAN_MEMBERS),
				DiscordUtils.hasPermission(context.getChannel(), context.getSelfId(), Permission.BAN_MEMBERS))
				.flatMap(tuple -> {
					final List<User> mentions = tuple.getT1();
					final Guild guild = tuple.getT2();
					final boolean hasUserPerm = tuple.getT3();
					final boolean hasBotPerm = tuple.getT4();
					
					if(!hasUserPerm) {
						throw new MissingPermissionException(Type.USER, Permission.BAN_MEMBERS);
					}

					if(!hasBotPerm) {
						throw new MissingPermissionException(Type.BOT, Permission.BAN_MEMBERS);
					}

					final StringBuilder reason = new StringBuilder();
					reason.append(StringUtils.remove(arg, FormatUtils.format(mentions, User::getMention, " ")).trim());
					if(reason.length() > DiscordUtils.MAX_REASON_LENGTH) {
						throw new CommandException(String.format("Reason cannot exceed **%d characters**.", DiscordUtils.MAX_REASON_LENGTH));
					}

					if(reason.length() == 0) {
						reason.append("Reason not specified.");
					}

					Flux<Void> softbanFlux = Flux.empty();
					for(User user : mentions) {
						if(!user.isBot()) {
							softbanFlux = softbanFlux.concatWith(BotUtils.sendMessage(
									String.format(Emoji.INFO + " You were softbanned from the server **%s** by **%s**. Reason: `%s`",
											guild.getName(), context.getUsername(), reason), user.getPrivateChannel().cast(MessageChannel.class))
									.then());
						}

						final BanQuerySpec banQuery = new BanQuerySpec()
								.setReason(reason.toString())
								.setDeleteMessageDays(7);
						softbanFlux = softbanFlux.concatWith(user.asMember(guildId)
								.flatMap(member -> member.ban(banQuery).then(member.unban())));
					}

					return softbanFlux
							.then(BotUtils.sendMessage(String.format(Emoji.INFO + " **%s** got softbanned by **%s**. Reason: `%s`",
									FormatUtils.format(mentions, User::getUsername, ", "), context.getUsername(), reason),
									context.getChannel()))
							.then();
				});
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Ban and instantly unban user(s).\nIt's like kicking him/them but it also deletes his/their messages "
						+ "from the last 7 days.")
				.addArg("@user(s)", false)
				.addArg("reason", true)
				.build();
	}

}
