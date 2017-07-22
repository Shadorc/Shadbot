package me.shadorc.discordbot.command;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.audio.AudioPlayer;

public class MusicPlayCommand extends Command {

	public MusicPlayCommand() {
		super("play", "joue");
	}

	@Override
	public void execute(Context context) {
		if(context.getArg() == null) {
			BotUtils.sendMessage("Merci d'entrer le nom de la chanson ou un mot.", context.getChannel());
			return;
		}

		IVoiceChannel botVoiceChannel = context.getClient().getOurUser().getVoiceStateForGuild(context.getGuild()).getChannel();

		if(botVoiceChannel == null) {
			BotUtils.sendMessage("Je ne suis dans aucun salon vocal, rejoignez-en un puis utilisez la command /join", context.getChannel());
			return;
		}

		// Get the AudioPlayer object for the guild
		AudioPlayer audioP = AudioPlayer.getAudioPlayerForGuild(context.getGuild());

		// Find a song given the search term
		File[] songDir = new File("S:/Bibliotheques/Music/Divers").listFiles(file -> file.getName().toLowerCase().contains(context.getArg().toLowerCase()));

		if(songDir == null || songDir.length == 0) {
			BotUtils.sendMessage("Aucune musique contenant " + context.getArg() + " n'a été trouvée.", context.getChannel());
			return;
		}

		// Stop the playing track
		audioP.clear();

		// Play the found song
		try {
			audioP.queue(songDir[0]);
		} catch (IOException | UnsupportedAudioFileException e) {
			Log.error("Une erreur est survenue lors de la lecture de la musique.", e, context.getChannel());
		}

		BotUtils.sendMessage("Lecture en cours : " + songDir[0].getName(), context.getChannel());
	}

}
