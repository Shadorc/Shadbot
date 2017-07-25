package me.shadorc.discordbot.command.music;

import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
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
			BotUtils.sendMessage(":grey_exclamation: Aucune musique en cours de lecture.", context.getChannel());
			return;
		}

		scheduler.setPaused(!scheduler.isPaused());
		if(scheduler.isPaused()) {
			BotUtils.sendMessage(":pause_button: Musique mise en pause par " + context.getAuthorName(), context.getChannel());
		} else {
			BotUtils.sendMessage(":arrow_forward: Reprise de la musique à la demande de " + context.getAuthorName(), context.getChannel());
		}
	}
}