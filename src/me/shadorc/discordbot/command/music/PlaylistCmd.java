package me.shadorc.discordbot.command.music;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.music.TrackScheduler;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Utils;

public class PlaylistCmd extends Command {

	public PlaylistCmd() {
		super(false, "playlist");
	}

	@Override
	public void execute(Context context) {
		GuildMusicManager musicManager = GuildMusicManager.getGuildAudioPlayer(context.getGuild());
		TrackScheduler scheduler = musicManager.getScheduler();

		if(!scheduler.isPlaying()) {
			BotUtils.sendMessage("Aucune musique en cours de lecture.", context.getChannel());
			return;
		}

		BotUtils.sendMessage(Utils.formatPlaylist(scheduler.getPlaylist()), context.getChannel());
	}
}