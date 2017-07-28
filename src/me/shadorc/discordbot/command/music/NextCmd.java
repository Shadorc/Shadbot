package me.shadorc.discordbot.command.music;

import me.shadorc.discordbot.Emoji;
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
			BotUtils.sendMessage(Emoji.WARNING + " Aucune musique en cours de lecture.", context.getChannel());
			return;
		}

		if(!scheduler.nextTrack()) {
			BotUtils.sendMessage(Emoji.WARNING + " Fin de la playlist.", context.getChannel());
			GuildMusicManager.getGuildAudioPlayer(context.getGuild()).leave();
		} else {
			BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " Musique suivante : *" + scheduler.getCurrentTrackName() + "*", context.getChannel());
		}
	}
}