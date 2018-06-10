// package me.shadorc.shadbot.command.info;
//
// import java.time.format.DateTimeFormatter;
// import java.util.List;
// import java.util.Locale;
//
// import discord4j.core.object.entity.Role;
// import discord4j.core.spec.EmbedCreateSpec;
// import me.shadorc.shadbot.core.command.AbstractCommand;
// import me.shadorc.shadbot.core.command.CommandCategory;
// import me.shadorc.shadbot.core.command.Context;
// import me.shadorc.shadbot.core.command.annotation.Command;
// import me.shadorc.shadbot.core.command.annotation.RateLimited;
// import me.shadorc.shadbot.exception.MissingArgumentException;
// import me.shadorc.shadbot.utils.BotUtils;
// import me.shadorc.shadbot.utils.FormatUtils;
// import me.shadorc.shadbot.utils.StringUtils;
// import me.shadorc.shadbot.utils.TimeUtils;
// import me.shadorc.shadbot.utils.embed.EmbedUtils;
// import me.shadorc.shadbot.utils.embed.HelpBuilder;
//
// @RateLimited
// @Command(category = CommandCategory.INFO, names = { "userinfo", "user_info", "user-info" })
// public class UserInfoCmd extends AbstractCommand {
//
// private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM uuuu - HH'h'mm", Locale.ENGLISH);
//
// @Override
// public void execute(Context context) {
// List<IUser> mentions = context.getMessage().getMentions();
// IUser user = mentions.isEmpty() ? context.getAuthor() : mentions.get(0);
//
// String creationDate = String.format("%s%n(%s)",
// TimeUtils.toLocalDate(user.getCreationDate()).format(dateFormatter),
// FormatUtils.formatLongDuration(user.getCreationDate()));
//
// String joinDate = String.format("%s%n(%s)",
// TimeUtils.toLocalDate(context.getGuild().getJoinTimeForUser(user)).format(dateFormatter),
// FormatUtils.formatLongDuration(context.getGuild().getJoinTimeForUser(user)));
//
// EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed(String.format("Info about user \"%s\"%s", user.getName(), user.isBot() ? " (Bot)" : ""))
// .withThumbnail(user.getAvatarURL())
// .addField("Display name", user.getDisplayName(context.getGuild()), true)
// .addField("User ID", Long.toString(user.getLongID()), true)
// .addField("Creation date", creationDate, true)
// .addField("Join date", joinDate, true)
// .addField("Roles", FormatUtils.format(user.getRolesForGuild(context.getGuild()), Role::getName, "\n"), true)
// .addField("Status", StringUtils.capitalize(user.getPresence().getStatus().toString()), true)
// .addField("Playing text", user.getPresence().getText().orElse(null), true);
// BotUtils.sendMessage(embed, context.getChannel());
// }
//
// @Override
// public EmbedCreateSpec getHelp(String prefix) {
// return new HelpBuilder(this, prefix)
// .setDescription("Show info about an user.")
// .addArg("@user", true)
// .build();
// }
//
// }
