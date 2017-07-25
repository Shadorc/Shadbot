package me.shadorc.discordbot.command.music;

import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.music.TrackScheduler;
import me.shadorc.discordbot.utility.BotUtils;

public class NextCmd extends Command {

	public NextCmd() {
		super(false, "next", "suivante");
	}

	@Override
	public void execute(Context context) {
		GuildMusicManager musicManager = GuildMusicManager.getGuildAudioPlayer(context.getGuild());
		TrackScheduler scheduler = musicManager.getScheduler();

		if(!scheduler.isPlaying()) {
			BotUtils.sendMessage(":grey_exclamation: Aucune musique en cours de lecture.", context.getChannel());
			return;
		}

		if(!scheduler.nextTrack()) {
			BotUtils.sendMessage(":grey_exclamation: Fin de la playlist.", context.getChannel());
			GuildMusicManager.getGuildAudioPlayer(context.getGuild()).leave();
		}
	}
}