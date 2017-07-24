package me.shadorc.discordbot.command.music;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.music.TrackScheduler;
import me.shadorc.discordbot.utility.BotUtils;

public class VolumeCmd extends Command {

	public VolumeCmd() {
		super(false, "volume");
	}

	@Override
	public void execute(Context context) {
		GuildMusicManager musicManager = GuildMusicManager.getGuildAudioPlayer(context.getGuild());
		TrackScheduler scheduler = musicManager.getScheduler();

		if(!scheduler.isPlaying()) {
			BotUtils.sendMessage("Aucune musique en cours de lecture.", context.getChannel());
			return;
		}

		if(context.getArg() == null) {
			BotUtils.sendMessage("Merci d'indiquer un volume compris entre 0 et 100.", context.getChannel());
			return;
		}

		try {
			scheduler.setVolume(Integer.parseInt(context.getArg()));
			BotUtils.sendMessage("Volume de la musique réglé sur " + scheduler.getVolume() + "%", context.getChannel());
		} catch (NumberFormatException e) {
			BotUtils.sendMessage("Merci d'indiquer un volume compris entre 0 et 100.", context.getChannel());
		}
	}

}
