package me.shadorc.discordbot.command.info;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;

import org.json.JSONArray;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.Setting;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.util.EmbedBuilder;

public class ServerInfoCmd extends AbstractCommand {

	private final DateTimeFormatter dateFormatter;

	public ServerInfoCmd() {
		super(Role.USER, "serverinfo", "server_info", "server-info");
		this.dateFormatter = DateTimeFormatter.ofPattern("d MMMM uuuu - HH'h'mm", Locale.ENGLISH);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		IGuild guild = context.getGuild();

		EmbedBuilder embed = new EmbedBuilder()
				.setLenient(true)
				.withAuthorName("Info about server " + guild.getName())
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withThumbnail(guild.getIconURL())
				.appendField("Owner", guild.getOwner().getName(), true)
				.appendField("Members", Integer.toString(guild.getTotalMemberCount()), true)
				.appendField("Creation date", guild.getCreationDate().format(dateFormatter), true)
				.appendField("Region", guild.getRegion().getName(), true)
				.appendField("Channels", Integer.toString(guild.getChannels().size()), true)
				.appendField("Voice channels", Integer.toString(guild.getVoiceChannels().size()), true)
				.appendField("Settings",
						"**Prefix:** " + Storage.getSetting(guild, Setting.PREFIX)
								+ "\n**Default volume:** " + Storage.getSetting(guild, Setting.DEFAULT_VOLUME) + "%"
								+ "\n**Allowed channels:** " + (Storage.getSetting(guild, Setting.ALLOWED_CHANNELS) == null ? "All" : "\n"
										+ Utils.convertArrayToList((JSONArray) Storage.getSetting(guild, Setting.ALLOWED_CHANNELS)).stream().map(
												idStr -> "\t" + guild.getChannelByID(Long.parseLong(idStr)).getName()).collect(Collectors.joining("\n"))), true)
				.appendField("Server ID", Long.toString(guild.getLongID()), true);
		BotUtils.sendEmbed(embed.build(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show info about this server.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
