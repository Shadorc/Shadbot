package me.shadorc.discordbot.command;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.music.GuildsMusicManager;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import sx.blah.discord.handle.obj.IVoiceChannel;

public class MusicPlayCmd extends Command {

	public MusicPlayCmd() {
		super("play", "joue");
	}

	@Override
	public void execute(Context context) {
		if(context.getArg() == null) {
			BotUtils.sendMessage("Merci d'entrer le nom ou une partie du nom de la chanson.", context.getChannel());
			return;
		}

		IVoiceChannel botVoiceChannel = context.getClient().getOurUser().getVoiceStateForGuild(context.getGuild()).getChannel();
		if(botVoiceChannel == null) {
			IVoiceChannel userVoiceChannel = context.getAuthor().getVoiceStateForGuild(context.getGuild()).getChannel();
			if(userVoiceChannel == null) {
				BotUtils.sendMessage("Rejoignez un salon vocal avant d'utiliser cette commande pour que je puisse vous rejoindre.", context.getChannel());
				return;
			}
			userVoiceChannel.join();
		}

		// Find a song given the search term
		File[] songDir = new File("S:/Bibliotheques/Music/Divers").listFiles(file -> file.getName().toLowerCase().contains(context.getArg().toLowerCase()));

		if(songDir == null || songDir.length == 0) {
			BotUtils.sendMessage("Aucune musique contenant " + context.getArg() + " n'a été trouvée.", context.getChannel());
			return;
		}

		GuildsMusicManager.getMusicPlayer(context.getGuild()).stop();

		// Play the found song
		try {
			GuildsMusicManager.getMusicPlayer(context.getGuild()).start(songDir[0]);
		} catch (IOException | UnsupportedAudioFileException e) {
			Log.error("Une erreur est survenue lors de la lecture de la musique.", e, context.getChannel());
		}

		BotUtils.sendMessage("Lecture en cours : " + songDir[0].getName(), context.getChannel());
	}

}
