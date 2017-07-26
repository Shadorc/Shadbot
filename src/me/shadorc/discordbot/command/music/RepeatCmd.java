package me.shadorc.discordbot.command.music;

import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.music.TrackScheduler;
import me.shadorc.discordbot.utility.BotUtils;

public class RepeatCmd extends Command {

	public RepeatCmd() {
		super(false, "repeat");
	}

	@Override
	public void execute(Context context) {
		GuildMusicManager musicManager = GuildMusicManager.getGuildAudioPlayer(context.getGuild());
		TrackScheduler scheduler = musicManager.getScheduler();

		if(!scheduler.isPlaying()) {
			BotUtils.sendMessage(":grey_exclamation: Aucune musique en cours de lecture.", context.getChannel());
			return;
		}

		scheduler.setRepeatEnabled(!scheduler.isRepeating());
		if(scheduler.isRepeating()) {
			BotUtils.sendMessage(":repeat: Répétition de la musique activée.", context.getChannel());
		} else {
			BotUtils.sendMessage(":arrow_forward: Répétition de la musique activée.", context.getChannel());
		}
	}

}
