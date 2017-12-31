package me.shadorc.shadbot.command.info;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.Database;
import me.shadorc.shadbot.data.db.DBGuild;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DateUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.util.EmbedBuilder;

@RateLimited
@Command(category = CommandCategory.INFO, names = { "serverinfo", "server_info", "server-info" })
public class ServerInfoCmd extends AbstractCommand {

	private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy - HH'h'mm", Locale.ENGLISH);

	@Override
	public void execute(Context context) throws MissingArgumentException {
		IGuild guild = context.getGuild();

		DBGuild dbGuild = Database.getDBGuild(guild);

		String allowedChannelsStr = "\n"
				+ FormatUtils.formatList("All", dbGuild.getAllowedChannels(), chlID -> "\t" + guild.getChannelByID(chlID).getName(), "\n");

		String blacklistedCmdStr = "\n"
				+ FormatUtils.formatList("None", dbGuild.getBlacklistedCmd(), cmdName -> "\t" + cmdName, "\n");

		String creationDate = String.format("%s%n(%s)",
				DateUtils.toLocalDate(guild.getCreationDate()).format(dateFormatter),
				FormatUtils.formatDate(guild.getCreationDate(), ChronoUnit.DAYS));

		EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
				.setLenient(true)
				.withAuthorName(String.format("Info about \"%s\"", guild.getName()))
				.withThumbnail(guild.getIconURL())
				.appendField("Owner", guild.getOwner().getName(), true)
				.appendField("Region", guild.getRegion().getName(), true)
				.appendField("Creation date", creationDate, true)
				.appendField("Members", Integer.toString(guild.getTotalMemberCount()), true)
				.appendField("Text channels", Integer.toString(guild.getChannels().size()), true)
				.appendField("Voice channels", Integer.toString(guild.getVoiceChannels().size()), true)
				.appendField("Settings", "**Prefix:** " + context.getPrefix()
						+ "\n**Default volume:** " + dbGuild.getDefaultVol() + "%"
						+ "\n**Allowed channels:** " + allowedChannelsStr
						+ "\n**Blacklisted command:** " + blacklistedCmdStr, true)
				.appendField("Server ID", Long.toString(guild.getLongID()), true);
		BotUtils.sendMessage(embed.build(), context.getChannel());
	}

	@Override
	public EmbedObject getHelp(Context context) {
		return new HelpBuilder(this, context.getPrefix())
				.setDescription("Show info about this server.")
				.build();
	}

}
