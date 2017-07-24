package me.shadorc.discordbot.command.music;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.music.TrackScheduler;
import me.shadorc.discordbot.utility.BotUtils;

public class PauseCmd extends Command {

	public PauseCmd() {
		super(false, "pause");
	}

	@Override
	public void execute(Context context) {
		GuildMusicManager musicManager = GuildMusicManager.getGuildAudioPlayer(context.getGuild());
		TrackScheduler scheduler = musicManager.getScheduler();

		if(!scheduler.isPlaying()) {
			BotUtils.sendMessage("Aucune musique en cours de lecture.", context.getChannel());
			return;
		}

		scheduler.setPaused(!scheduler.isPaused());
	}
}