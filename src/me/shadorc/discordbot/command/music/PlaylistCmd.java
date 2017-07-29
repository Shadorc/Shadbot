package me.shadorc.discordbot.command.music;

import java.awt.Color;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.music.TrackScheduler;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Utils;
import sx.blah.discord.util.EmbedBuilder;

public class PlaylistCmd extends Command {

	public PlaylistCmd() {
		super(false, "playlist");
	}

	@Override
	public void execute(Context context) {
		GuildMusicManager musicManager = GuildMusicManager.getGuildAudioPlayer(context.getGuild());
		TrackScheduler scheduler = musicManager.getScheduler();

		if(!scheduler.isPlaying()) {
			BotUtils.sendMessage(Emoji.WARNING + " Aucune musique en cours de lecture.", context.getChannel());
			return;
		}

		BotUtils.sendMessage(Utils.formatPlaylist(scheduler.getPlaylist()), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Aide pour la commande /" + context.getArg())
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(new Color(170, 196, 222))
				.appendDescription("**Affiche la playlist en cours.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}