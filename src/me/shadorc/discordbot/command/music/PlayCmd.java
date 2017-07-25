package me.shadorc.discordbot.command.music;

import java.io.File;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.discordbot.utility.Utils;
import sx.blah.discord.handle.obj.IVoiceChannel;

public class PlayCmd extends Command {

	public PlayCmd() {
		super(false, "play", "joue");
	}

	@Override
	public void execute(Context context) {
		if(context.getArg() == null) {
			BotUtils.sendMessage(":grey_exclamation: Merci d'entrer l'URL d'une musique à écouter.", context.getChannel());
			return;
		}

		IVoiceChannel botVoiceChannel = context.getClient().getOurUser().getVoiceStateForGuild(context.getGuild()).getChannel();
		IVoiceChannel userVoiceChannel = context.getAuthor().getVoiceStateForGuild(context.getGuild()).getChannel();

		if(botVoiceChannel == null) {
			if(userVoiceChannel == null) {
				BotUtils.sendMessage(":grey_exclamation: Rejoignez un salon vocal avant d'utiliser cette commande pour que je puisse vous rejoindre.", context.getChannel());
				return;
			}
			userVoiceChannel.join();
		}

		String identifier = context.getArg();
		if(!Utils.isValidURL(identifier)) {
			File[] songDir = new File("S:/Bibliotheques/Music/Divers").listFiles(file -> file.getName().toLowerCase().contains(context.getArg().toLowerCase()));
			if(songDir == null || songDir.length == 0) {
				BotUtils.sendMessage(":grey_exclamation: Aucune musique contenant " + context.getArg() + " n'a été trouvée.", context.getChannel());
				return;
			}
			identifier = songDir[0].getPath();
		}

		GuildMusicManager musicManager = GuildMusicManager.getGuildAudioPlayer(context.getGuild());
		musicManager.setRequestedChannel(context.getChannel());
		GuildMusicManager.PLAYER_MANAGER.loadItemOrdered(musicManager, identifier, new AudioLoadResultHandler() {
			@Override
			public void trackLoaded(AudioTrack track) {
				BotUtils.sendMessage(":musical_note: Ajout de *" + Utils.formatTrackName(track.getInfo()) + "* à la playlist.", context.getChannel());
				musicManager.getScheduler().queue(track);
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist) {
				for(AudioTrack track : playlist.getTracks()) {
					musicManager.getScheduler().queue(track);
				}
				BotUtils.sendMessage(":musical_note: Toutes les musiques ont été ajoutés à la playlist, elle contient maintenant " + musicManager.getScheduler().getPlaylist().size() + " éléments.", context.getChannel());
				BotUtils.sendMessage(":musical_note: Lecture en cours : *" + musicManager.getScheduler().getCurrentTrackName() + "*", context.getChannel());
			}

			@Override
			public void noMatches() {
				BotUtils.sendMessage(":heavy_multiplication_x: Aucun résultat n'a été trouvé pour " + context.getArg(), context.getChannel());
			}

			@Override
			public void loadFailed(FriendlyException e) {
				Log.error("Le chargement de la musique a échoué.", e, context.getChannel());
			}
		});
	}
}
