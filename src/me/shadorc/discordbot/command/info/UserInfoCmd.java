package me.shadorc.discordbot.command.info;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class UserInfoCmd extends AbstractCommand {

	private final DateTimeFormatter dateFormatter;
	private final RateLimiter rateLimiter;

	public UserInfoCmd() {
		super(CommandCategory.INFO, Role.USER, "userinfo", "user_info", "user-info");
		this.rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
		this.dateFormatter = DateTimeFormatter.ofPattern("d MMMM uuuu - HH'h'mm", Locale.ENGLISH);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		IUser user = context.getMessage().getMentions().isEmpty() ? context.getAuthor() : context.getMessage().getMentions().get(0);

		EmbedBuilder embed = Utils.getDefaultEmbed()
				.setLenient(true)
				.withAuthorName("Info about " + user.getName() + (user.isBot() ? " (Bot)" : ""))
				.withThumbnail(user.getAvatarURL())
				.appendField("Display name", user.getDisplayName(context.getGuild()), true)
				.appendField("User ID", Long.toString(user.getLongID()), true)
				.appendField("Creation date", user.getCreationDate().format(dateFormatter)
						+ "\n(" + StringUtils.formateDate(user.getCreationDate()) + ")", true)
				.appendField("Join date", context.getGuild().getJoinTimeForUser(user).format(dateFormatter)
						+ "\n(" + StringUtils.formateDate(context.getGuild().getJoinTimeForUser(user)) + ")", true)
				.appendField("Status", user.getPresence().getStatus().toString(), true)
				.appendField("Playing text", user.getPresence().getPlayingText().orElse(null), true)
				.appendField("Roles", StringUtils.formatList(user.getRolesForGuild(context.getGuild()), role -> role.getName(), "\n"), true);
		BotUtils.sendMessage(embed.build(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show info about an user.**")
				.appendField("Usage", "`" + context.getPrefix() + "userinfo <@user>`", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

}
