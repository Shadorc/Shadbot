package me.shadorc.discordbot.command.info;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class UserInfoCmd extends AbstractCommand {

	private final DateTimeFormatter dateFormatter;

	public UserInfoCmd() {
		super(Role.USER, "userinfo", "user_info");
		this.dateFormatter = DateTimeFormatter.ofPattern("d MMMM uuuu - HH'h'mm", Locale.ENGLISH);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		IUser user = context.getMessage().getMentions().isEmpty() ? context.getAuthor() : context.getMessage().getMentions().get(0);

		EmbedBuilder embed = new EmbedBuilder()
				.setLenient(true)
				.withAuthorName("Info about " + user.getName() + (user.isBot() ? " (Bot)" : ""))
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withThumbnail(user.getAvatarURL())
				.appendField("Display name", user.getDisplayName(context.getGuild()), true)
				.appendField("ID", Long.toString(user.getLongID()), true)
				.appendField("Creation date", user.getCreationDate().format(dateFormatter), true)
				.appendField("Join date", context.getGuild().getJoinTimeForUser(user).format(dateFormatter), true)
				.appendField("Status", user.getPresence().getStatus().toString(), true)
				.appendField("Playing text", user.getPresence().getPlayingText().orElse(null), true)
				.appendField("Roles", user.getRolesForGuild(context.getGuild()).toString().replace("[", "").replace("]", ""), true);
		BotUtils.sendEmbed(embed.build(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show info about an user.**")
				.appendField("Usage", context.getPrefix() + "userinfo <@user>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
