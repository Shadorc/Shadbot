package me.shadorc.discordbot.command.info;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.Storage;
import me.shadorc.discordbot.data.Storage.Setting;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.util.EmbedBuilder;

public class ServerInfoCmd extends AbstractCommand {

	private final DateTimeFormatter dateFormatter;
	private final RateLimiter rateLimiter;

	public ServerInfoCmd() {
		super(CommandCategory.INFO, Role.USER, "serverinfo", "server_info", "server-info");
		this.rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
		this.dateFormatter = DateTimeFormatter.ofPattern("d MMMM uuuu - HH'h'mm", Locale.ENGLISH);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		IGuild guild = context.getGuild();
		List<Long> allowedChannels = Utils.convertToLongList((JSONArray) Storage.getSetting(guild, Setting.ALLOWED_CHANNELS));

		EmbedBuilder embed = Utils.getDefaultEmbed()
				.setLenient(true)
				.withAuthorName("Info about \"" + guild.getName() + "\"")
				.withThumbnail(guild.getIconURL())
				.appendField("Owner", guild.getOwner().getName(), true)
				.appendField("Members", Integer.toString(guild.getTotalMemberCount()), true)
				.appendField("Region", guild.getRegion().getName(), true)
				.appendField("Creation date", guild.getCreationDate().format(dateFormatter)
						+ "\n(" + StringUtils.formateDate(guild.getCreationDate()) + ")", true)
				.appendField("Channels", Integer.toString(guild.getChannels().size()), true)
				.appendField("Voice channels", Integer.toString(guild.getVoiceChannels().size()), true)
				.appendField("Settings",
						"**Prefix:** " + context.getPrefix()
								+ "\n**Default volume:** " + Storage.getSetting(guild, Setting.DEFAULT_VOLUME) + "%"
								+ "\n**Allowed channels:** " + (allowedChannels.isEmpty() ? "All" : "\n"
										+ StringUtils.formatList(
												allowedChannels,
												channelID -> "\t" + guild.getChannelByID(channelID).getName(),
												"\n")), true)
				.appendField("Server ID", Long.toString(guild.getLongID()), true);
		BotUtils.send(embed.build(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show info about this server.**");
		BotUtils.send(builder.build(), context.getChannel());
	}

}
