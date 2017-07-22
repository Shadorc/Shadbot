package me.shadorc.discordbot.command;

import java.io.File;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.discordbot.utility.Utils;
import sx.blah.discord.handle.obj.IVoiceChannel;

public class MusicPlayCmd extends Command {

	public MusicPlayCmd() {
		super("play", "joue");
	}

	@Override
	public void execute(Context context) {
		if(context.getArg() == null) {
			BotUtils.sendMessage("Merci d'entrer l'URL d'une musique à écouter.", context.getChannel());
			return;
		}

		IVoiceChannel botVoiceChannel = context.getClient().getOurUser().getVoiceStateForGuild(context.getGuild()).getChannel();
		IVoiceChannel userVoiceChannel = context.getAuthor().getVoiceStateForGuild(context.getGuild()).getChannel();

		if(botVoiceChannel == null) {
			if(userVoiceChannel == null) {
				BotUtils.sendMessage("Rejoignez un salon vocal avant d'utiliser cette commande pour que je puisse vous rejoindre.", context.getChannel());
				return;
			}
			userVoiceChannel.join();
		}

		String identifier = context.getArg();
		if(!Utils.isValidURL(identifier)) {
			File[] songDir = new File("S:/Bibliotheques/Music/Divers").listFiles(file -> file.getName().toLowerCase().contains(context.getArg().toLowerCase()));
			if(songDir == null || songDir.length == 0) {
				BotUtils.sendMessage("Aucune musique contenant " + context.getArg() + " n'a été trouvée.", context.getChannel());
				return;
			}
			identifier = songDir[0].getPath();
		}

		GuildMusicManager musicManager = GuildMusicManager.getGuildAudioPlayer(context.getGuild());
		GuildMusicManager.getAudioPlayerManager().loadItemOrdered(musicManager, identifier, new AudioLoadResultHandler() {
			@Override
			public void trackLoaded(AudioTrack track) {
				BotUtils.sendMessage("Ajout de *" + track.getInfo().author + " - " + track.getInfo().title + "* à la playlist.", context.getChannel());
				musicManager.getScheduler().queue(track);
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist) {
				for(AudioTrack track : playlist.getTracks()) {
					BotUtils.sendMessage("Ajout de *" + track.getInfo().author + " - " + track.getInfo().title + "* à la playlist.", context.getChannel());
					musicManager.getScheduler().queue(track);
				}
			}

			@Override
			public void noMatches() {
				BotUtils.sendMessage("Aucun résultat n'a été trouvé pour " + context.getArg(), context.getChannel());
			}

			@Override
			public void loadFailed(FriendlyException e) {
				Log.error("Le chargement de la musique a échoué.", e, context.getChannel());
			}
		});
	}
}
