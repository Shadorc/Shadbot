package me.shadorc.shadbot.command.info;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
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
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.publisher.Flux;

@RateLimited
@Command(category = CommandCategory.INFO, names = { "serverinfo", "server_info", "server-info" })
public class ServerInfoCmd extends AbstractCommand {

	private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy - HH'h'mm", Locale.ENGLISH);

	@Override
	public void execute(Context context) throws MissingArgumentException {

		DBGuild dbGuild = Database.getDBGuild(context.getGuildId().get());

		StringBuilder settingsStr = new StringBuilder();

		Flux<String> holyShit = Flux.empty();

		if(!dbGuild.getPrefix().equals(Config.DEFAULT_PREFIX)) {
			settingsStr.append(String.format("**Prefix:** %s", context.getPrefix()));
		}

		if(dbGuild.getDefaultVol().intValue() != Config.DEFAULT_VOLUME) {
			settingsStr.append(String.format("%n**Default volume:** %d%%", dbGuild.getDefaultVol()));
		}

		if(!dbGuild.getAllowedChannels().isEmpty()) {
			Flux.fromIterable(dbGuild.getAllowedChannels())
					.flatMap(context.getClient()::getTextChannelById)
					.map(TextChannel::getName)
					.buffer()
					.doOnNext(list -> settingsStr.append(
							String.format("%n**Allowed channels:**%n\t%s", FormatUtils.format(list, Object::toString, "\n\t"))))
					.subscribe();
		}

		if(!dbGuild.getBlacklistedCmd().isEmpty()) {
			settingsStr.append(String.format("%n**Blacklisted commands:**%n\t%s",
					FormatUtils.format(dbGuild.getBlacklistedCmd(), Object::toString, "\n\t")));
		}

		if(!dbGuild.getAutoRoles().isEmpty()) {
			Flux.fromIterable(dbGuild.getAutoRoles())
					.flatMap(roleId -> context.getClient().getRoleById(context.getGuildId().get(), roleId))
					.map(Role::getMention)
					.buffer()
					.doOnNext(list -> settingsStr.append(
							String.format("%n**Auto-roles:**%n\t%s", FormatUtils.format(list, Object::toString, "\n\t"))))
					.subscribe();
		}

		if(!dbGuild.getAllowedRoles().isEmpty()) {
			Flux.fromIterable(dbGuild.getAllowedRoles())
					.flatMap(roleId -> context.getClient().getRoleById(context.getGuildId().get(), roleId))
					.map(Role::getMention)
					.buffer()
					.doOnNext(list -> settingsStr.append(
							String.format("%n**Permissions:**%n\t%s", FormatUtils.format(list, Object::toString, "\n\t"))));
		}

		context.getGuild().subscribe(guild -> {
			String creationDate = String.format("%s%n(%s)",
					TimeUtils.toLocalDate(Utils.getSnowflakeTimeFromID(guild.getId())).format(dateFormatter),
					FormatUtils.formatLongDuration(Utils.getSnowflakeTimeFromID(guild.getId())));

			EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed(String.format("Info about \"%s\"", guild.getName()))
					.setThumbnail(guild.getIconHash().orElse(""))
					.addField("Owner", guild.getOwner().getName(), true)
					.addField("Server ID", Long.toString(guild.getLongID()), true)
					.addField("Creation date", creationDate, true)
					.addField("Region", guild.getRegion().getName(), true)
					.addField("Channels", String.format("**Voice:** %d", guild.getVoiceChannels().size())
							+ String.format("%n**Text:** %d", guild.getChannels().size()), true)
					.addField("Members", Integer.toString(guild.getTotalMemberCount()), true)
					.addField("Settings", settingsStr.toString(), true);
			BotUtils.sendMessage(embed, context.getChannel());
		});
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show info about this server.")
				.build();
	}

}
