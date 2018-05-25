package me.shadorc.shadbot.command.admin;

import java.util.Set;

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
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;

@Command(category = CommandCategory.ADMIN, permission = CommandPermission.ADMIN, names = { "kick" })
public class KickCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		context.requireArg();

		Set<Snowflake> mentionedUserIds = context.getMessage().getUserMentionIds();
		if(mentionedUserIds.isEmpty()) {
			throw new MissingArgumentException();
		}

		Snowflake guildId = context.getGuildId().get();

		Utils.hasPermissions(context.getAuthor(), guildId, Permission.KICK_MEMBERS).subscribe(canUserKick -> {

			if(!canUserKick) {
				throw new IllegalArgumentException("You don't have permission to kick.");
			}

			Utils.hasPermissions(context.getSelf(), guildId, Permission.KICK_MEMBERS).subscribe(canBotKick -> {

				if(!canBotKick) {
					BotUtils.sendMessage(TextUtils.missingPerm(Permission.KICK_MEMBERS), context.getChannel());
					return;
				}

				if(mentionedUserIds.contains(context.getAuthorId())) {
					throw new IllegalCmdArgumentException("You cannot kick yourself.");
				}

				// TODO
				// for(IUser mentionedUser : mentionedUsers) {
				// if(!PermissionUtils.isUserHigher(context.getGuild(), context.getAuthor(), mentionedUser)) {
				// throw new IllegalCmdArgumentException(String.format("You can't kick **%s** because he has the same or a higher role "
				// + "position than you in the role hierarchy.",
				// mentionedUser.getName()));
				// }
				// if(!BotUtils.canInteract(context.getGuild(), mentionedUser)) {
				// throw new IllegalCmdArgumentException(String.format("I cannot kick **%s** because he has the same or a higher role "
				// + "position than me in the role hierarchy.",
				// mentionedUser.getName()));
				// }
				// }

				context.getMessage().getUserMentions().buffer().subscribe(mentionedUsers -> {

					StringBuilder reason = new StringBuilder();
					reason.append(StringUtils.remove(context.getArg().get(),
							FormatUtils.format(mentionedUsers, User::getMention, " ")).trim());
					if(reason.length() > Utils.MAX_REASON_LENGTH) {
						throw new IllegalCmdArgumentException(String.format("Reason cannot exceed **%d characters**.", Utils.MAX_REASON_LENGTH));
					}

					if(reason.length() == 0) {
						reason.append("Reason not specified.");
					}

					context.getGuild().subscribe(guild -> {
						context.getAuthor().map(User::getUsername).subscribe(authorName -> {

							for(User user : mentionedUsers) {
								if(!user.isBot()) {
									BotUtils.sendMessage(String.format(Emoji.INFO + " You were kicked from the server **%s** by **%s**. Reason: `%s`",
											guild.getName(), authorName, reason), user.getPrivateChannel().cast(MessageChannel.class));
								}

								// TODO: Add reason
								context.getMember()
										.doOnError(ExceptionUtils::isForbidden, err -> {
											BotUtils.sendMessage(TextUtils.missingPerm(Permission.KICK_MEMBERS), context.getChannel());
											LogUtils.infof("{Guild ID: %d} Shadbot wasn't allowed to kick a member.", guildId);
										})
										.subscribe(member -> {
											member.kick();
										});
							}

							BotUtils.sendMessage(String.format(Emoji.INFO + " (Requested by **%s**) **%s** got kicked. Reason: `%s`",
									authorName,
									FormatUtils.format(mentionedUsers, User::getUsername, ", "),
									reason), context.getChannel());
						});
					});
				});
			});
		});
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Kick user(s).")
				.addArg("@user(s)", false)
				.addArg("reason", true)
				.build();
	}

}
