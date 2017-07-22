package me.shadorc.discordbot.command;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.music.MusicManager;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;

public class MusicVolumeCommand extends Command {

	public MusicVolumeCommand() {
		super("volume");
	}

	@Override
	public void execute(Context context) {
		try {
			MusicManager.getMusicPlayer(context.getGuild()).setVolume(Integer.parseInt(context.getArg()));
			BotUtils.sendMessage("Volume de la musique réglé sur " + MusicManager.getMusicPlayer(context.getGuild()).getVolume() + ".", context.getChannel());
		} catch (NumberFormatException e) {
			Log.error("Merci d'indiquer un volume compris entre 1 et 100.", e, context.getChannel());
		}
	}
}
