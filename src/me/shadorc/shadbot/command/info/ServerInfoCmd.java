package me.shadorc.shadbot.command.info;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.db.DBGuild;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;

@RateLimited
@Command(category = CommandCategory.INFO, names = { "serverinfo", "server_info", "server-info" })
public class ServerInfoCmd extends AbstractCommand {

	private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy - HH'h'mm", Locale.ENGLISH);

	@Override
	public void execute(Context context) throws MissingArgumentException {
		IGuild guild = context.getGuild();

		DBGuild dbGuild = Database.getDBGuild(guild);

		StringBuilder settingsStr = new StringBuilder();
		if(!dbGuild.getPrefix().equals(Config.DEFAULT_PREFIX)) {
			settingsStr.append(String.format("**Prefix:** %s", context.getPrefix()));
		}
		if(dbGuild.getDefaultVol() != Config.DEFAULT_VOLUME) {
			settingsStr.append(String.format("%n**Default volume:** %d%%", dbGuild.getDefaultVol()));
		}
		if(!dbGuild.getAllowedChannels().isEmpty()) {
			List<IChannel> channels = dbGuild.getAllowedChannels().stream()
					.map(guild::getChannelByID)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			settingsStr.append(String.format("%n**Allowed channels:**%n\t%s", FormatUtils.format(channels, IChannel::getName, "\n\t")));
		}
		if(!dbGuild.getBlacklistedCmd().isEmpty()) {
			settingsStr.append(String.format("%n**Blacklisted commands:**%n\t%s",
					FormatUtils.format(dbGuild.getBlacklistedCmd(), Object::toString, "\n\t")));
		}
		if(!dbGuild.getAutoRoles().isEmpty()) {
			List<IRole> roles = dbGuild.getAutoRoles().stream()
					.map(guild::getRoleByID)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			settingsStr.append(String.format("%n**Auto-roles:**%n\t%s", FormatUtils.format(roles, IRole::mention, "\n\t")));
		}
		if(!dbGuild.getAllowedRoles().isEmpty()) {
			List<IRole> allowedRoles = dbGuild.getAllowedRoles().stream()
					.map(guild::getRoleByID)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			settingsStr.append(String.format("%n**Permissions:**%n\t%s", FormatUtils.format(allowedRoles, IRole::mention, "\n\t")));
		}

		String creationDate = String.format("%s%n(%s)",
				TimeUtils.toLocalDate(guild.getCreationDate()).format(dateFormatter),
				FormatUtils.formatLongDuration(guild.getCreationDate()));

		EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
				.setLenient(true)
				.withAuthorName(String.format("Info about \"%s\"", guild.getName()))
				.withThumbnail(guild.getIconURL())
				.addField("Owner", guild.getOwner().getName(), true)
				.addField("Server ID", Long.toString(guild.getLongID()), true)
				.addField("Creation date", creationDate, true)
				.addField("Region", guild.getRegion().getName(), true)
				.addField("Channels", String.format("**Voice:** %d", guild.getVoiceChannels().size())
						+ String.format("%n**Text:** %d", guild.getChannels().size()), true)
				.addField("Members", Integer.toString(guild.getTotalMemberCount()), true)
				.addField("Settings", settingsStr.toString(), true);
		BotUtils.sendMessage(embed.build(), context.getChannel());
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show info about this server.")
				.build();
	}

}
