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
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@Command(category = CommandCategory.ADMIN, permission = CommandPermission.ADMIN, names = { "ban" })
public class BanCmd extends AbstractCommand {

	@Override
	public void execute(Context context) {
		context.requireArg();

		Set<Snowflake> mentionedUserIds = context.getMessage().getUserMentionIds();
		if(mentionedUserIds.isEmpty()) {
			throw new MissingArgumentException();
		}

		DiscordUtils.hasPermissions(context.getMember(), Permission.BAN_MEMBERS)
				.subscribe(canUserBan -> {

					if(!canUserBan) {
						throw new IllegalArgumentException("You don't have permission to ban.");
					}

					final Snowflake guildId = context.getGuildId().get();

					DiscordUtils.hasPermissions(context.getSelf(), guildId, Permission.BAN_MEMBERS)
							.subscribe(canBotBan -> {

								if(!canBotBan) {
									BotUtils.sendMessage(TextUtils.missingPerm(Permission.BAN_MEMBERS), context.getChannel());
									return;
								}

								if(mentionedUserIds.contains(context.getAuthorId())) {
									throw new IllegalCmdArgumentException("You cannot ban yourself.");
								}

								context.getMessage().getUserMentions().buffer().subscribe(mentionedUsers -> {
									StringBuilder reason = new StringBuilder();
									reason.append(StringUtils.remove(context.getArg().get(), FormatUtils.format(mentionedUsers, User::getMention, " ")).trim());
									if(reason.length() > DiscordUtils.MAX_REASON_LENGTH) {
										throw new IllegalCmdArgumentException(String.format("Reason cannot exceed **%d characters**.", DiscordUtils.MAX_REASON_LENGTH));
									}

									if(reason.length() == 0) {
										reason.append("Reason not specified.");
									}

									context.getGuild().subscribe(guild -> {
										context.getAuthor().map(User::getUsername).subscribe(authorName -> {

											for(User user : mentionedUsers) {
												if(!user.isBot()) {
													BotUtils.sendMessage(String.format(Emoji.INFO + " You were banned from the server **%s** by **%s**. Reason: `%s`",
															guild.getName(), authorName, reason), user.getPrivateChannel().cast(MessageChannel.class));
												}

												// TODO: Add reason
												user.asMember(guildId)
														.doOnError(ExceptionUtils::isForbidden, err -> {
															BotUtils.sendMessage(TextUtils.missingPerm(Permission.BAN_MEMBERS), context.getChannel());
															LogUtils.infof("{Guild ID: %d} Shadbot wasn't allowed to ban a member.", guildId);
														})
														.subscribe(/* TODO Member::ban */);
											}

											BotUtils.sendMessage(String.format(Emoji.INFO + " (Requested by **%s**) **%s** got banned. Reason: `%s`",
													authorName, FormatUtils.format(mentionedUsers, User::getUsername, ", "), reason), context.getChannel());
										});
									});
								});
							});
				});

	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Ban user(s) and delete his/their messages from the last 7 days.")
				.addArg("@user(s)", false)
				.addArg("reason", true)
				.build();
	}

}
