package me.shadorc.discordbot.command;


import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.music.TrackScheduler;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.discordbot.utility.Utils;

public class MusicCmd extends Command {

	public MusicCmd() {
		super("music", "musique");
	}

	@Override
	public void execute(Context context) {
		GuildMusicManager musicManager = GuildMusicManager.getGuildAudioPlayer(context.getGuild());
		TrackScheduler scheduler = musicManager.getScheduler();

		if(!scheduler.isPlaying()) {
			BotUtils.sendMessage("Aucune musique en cours de lecture.", context.getChannel());
			return;
		}

		String[] splitCmd = context.getArg().split(" ", 2);
		if(splitCmd.length == 0) {
			BotUtils.sendMessage("Merci d'indiquer une commande valide.", context.getChannel());
			return;
		}

		String subCmd = splitCmd[0].toLowerCase().trim();
		String subArg = null;
		if(splitCmd.length > 1) {
			subArg = splitCmd[1].toLowerCase().trim();
		}

		if(subCmd.equals("volume")) {
			if(subArg == null) {
				BotUtils.sendMessage("Merci d'indiquer un volume compris entre 1 et 100.", context.getChannel());
				return;
			}
			try {
				scheduler.setVolume(Integer.parseInt(subArg));
				BotUtils.sendMessage("Volume de la musique réglé sur " + scheduler.getVolume() + "%", context.getChannel());
			} catch (NumberFormatException e) {
				BotUtils.sendMessage("Merci d'indiquer un volume compris entre 1 et 100.", context.getChannel());
			}
		}

		else if(subCmd.equals("pause")) {
			scheduler.setPaused(!scheduler.isPaused());
		}

		else if(subCmd.equals("stop")) {
			scheduler.stop();
		}

		else if(subCmd.equals("next")) {
			scheduler.nextTrack();
		}

		else if(subCmd.equals("name")) {
			BotUtils.sendMessage("Musique en cours : " + scheduler.getCurrentTrackName(), context.getChannel());
		}

		else if(subCmd.equals("playlist")) {
			BotUtils.sendMessage(Utils.formatPlaylist(scheduler.getPlaylist()), context.getChannel());
		}

		else {
			BotUtils.sendMessage("Cette commande est inconnue pour la musique, tapez /help pour plus d'informations.", context.getChannel());
			Log.error("La commande musicale " + subCmd + " a été utilisée sans résultat.");
		}

	}

}
