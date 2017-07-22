package me.shadorc.discordbot.command;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.music.GuildsMusicManager;
import me.shadorc.discordbot.music.MusicManager;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.discordbot.utility.Utils;

public class MusicCmd extends Command {

	public MusicCmd() {
		super("music", "musique");
	}

	@Override
	public void execute(Context context) {
		MusicManager musicManager = GuildsMusicManager.getMusicManager(context.getGuild());

		if(!musicManager.isPlaying()) {
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
				musicManager.setVolume(Integer.parseInt(subArg));
				BotUtils.sendMessage("Volume de la musique réglé sur " + musicManager.getVolume() + "%", context.getChannel());
			} catch (NumberFormatException e) {
				BotUtils.sendMessage("Merci d'indiquer un volume compris entre 1 et 100.", context.getChannel());
			}
		}

		else if(subCmd.equals("pause")) {
			musicManager.setPaused(!musicManager.isPaused());
		}

		else if(subCmd.equals("stop")) {
			musicManager.stop();
		}

		else if(subCmd.equals("next")) {
			musicManager.next();
		}

		else if(subCmd.equals("name")) {
			BotUtils.sendMessage("Musique en cours : " + musicManager.getCurrentTrackName(), context.getChannel());
		}

		else if(subCmd.equals("playlist")) {
			BotUtils.sendMessage(Utils.formatPlaylist(musicManager.getPlaylist()), context.getChannel());
		}

		else {
			BotUtils.sendMessage("Cette commande est inconnue pour la musique, tapez /help pour plus d'informations.", context.getChannel());
			Log.error("La commande musicale " + subCmd + " a été utilisée sans résultat.");
		}

	}

}
