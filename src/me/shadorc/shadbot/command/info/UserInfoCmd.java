package me.shadorc.shadbot.command.info;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DateUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

@RateLimited
@Command(category = CommandCategory.INFO, names = { "userinfo", "user_info", "user-info" })
public class UserInfoCmd extends AbstractCommand {

	private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM uuuu - HH'h'mm", Locale.ENGLISH);

	@Override
	public void execute(Context context) throws MissingArgumentException {
		List<IUser> mentions = context.getMessage().getMentions();
		IUser user = mentions.isEmpty() ? context.getAuthor() : mentions.get(0);

		String creationDate = String.format("%s%n(%s)",
				DateUtils.toLocalDate(user.getCreationDate()).format(dateFormatter),
				FormatUtils.formatLongDuration(user.getCreationDate().toEpochMilli()));

		String joinDate = String.format("%s%n(%s)",
				DateUtils.toLocalDate(context.getGuild().getJoinTimeForUser(user)).format(dateFormatter),
				FormatUtils.formatLongDuration(context.getGuild().getJoinTimeForUser(user).toEpochMilli()));

		EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
				.setLenient(true)
				.withAuthorName(String.format("Info about user \"%s\"%s", user.getName(), user.isBot() ? " (Bot)" : ""))
				.withThumbnail(user.getAvatarURL())
				.appendField("Display name", user.getDisplayName(context.getGuild()), true)
				.appendField("User ID", Long.toString(user.getLongID()), true)
				.appendField("Creation date", creationDate, true)
				.appendField("Join date", joinDate, true)
				.appendField("Roles", FormatUtils.format(user.getRolesForGuild(context.getGuild()), IRole::getName, "\n"), true)
				.appendField("Status", StringUtils.capitalize(user.getPresence().getStatus().toString()), true)
				.appendField("Playing text", user.getPresence().getText().orElse(null), true);
		BotUtils.sendMessage(embed.build(), context.getChannel());
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show info about an user.")
				.addArg("@user", true)
				.build();
	}

}
