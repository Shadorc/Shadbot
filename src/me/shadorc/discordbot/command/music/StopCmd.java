package me.shadorc.discordbot.command.music;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.music.TrackScheduler;
import me.shadorc.discordbot.utility.BotUtils;

public class StopCmd extends Command {

	public StopCmd() {
		super(false, "stop");
	}

	@Override
	public void execute(Context context) {
		GuildMusicManager musicManager = GuildMusicManager.getGuildAudioPlayer(context.getGuild());
		TrackScheduler scheduler = musicManager.getScheduler();

		if(!scheduler.isPlaying()) {
			BotUtils.sendMessage(Emoji.WARNING + " Aucune musique en cours de lecture.", context.getChannel());
			return;
		}

		BotUtils.sendMessage(Emoji.WARNING + " L'écoute des musiques a été arrêté par " + context.getAuthorName() +".", context.getChannel());
		GuildMusicManager.getGuildAudioPlayer(context.getGuild()).leave();
	}
}