package me.shadorc.discordbot.command.music;

import java.awt.Color;
import java.io.File;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.discordbot.utility.Utils;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.EmbedBuilder;

public class PlayCmd extends Command {

	public PlayCmd() {
		super(false, "play", "joue");
	}

	@Override
	public void execute(Context context) {
		if(context.getArg() == null) {
			BotUtils.sendMessage(Emoji.WARNING + " Merci d'entrer l'URL d'une musique à écouter.", context.getChannel());
			return;
		}

		IVoiceChannel botVoiceChannel = context.getClient().getOurUser().getVoiceStateForGuild(context.getGuild()).getChannel();
		IVoiceChannel userVoiceChannel = context.getAuthor().getVoiceStateForGuild(context.getGuild()).getChannel();

		if(botVoiceChannel == null) {
			if(userVoiceChannel == null) {
				BotUtils.sendMessage(Emoji.WARNING + " Rejoignez un salon vocal avant d'utiliser cette commande pour que je puisse vous rejoindre.", context.getChannel());
				return;
			}
			userVoiceChannel.join();
		}

		String identifier = context.getArg();
		if(!Utils.isValidURL(identifier)) {
			File[] songDir = new File("S:/Bibliotheques/Music/Divers").listFiles(file -> file.getName().toLowerCase().contains(context.getArg().toLowerCase()));
			if(songDir == null || songDir.length == 0) {
				BotUtils.sendMessage(Emoji.WARNING + " Aucune musique contenant " + context.getArg() + " n'a été trouvée.", context.getChannel());
				return;
			}
			identifier = songDir[0].getPath();
		}

		GuildMusicManager musicManager = GuildMusicManager.getGuildAudioPlayer(context.getGuild());
		musicManager.setRequestedChannel(context.getChannel());
		GuildMusicManager.PLAYER_MANAGER.loadItemOrdered(musicManager, identifier, new AudioLoadResultHandler() {
			@Override
			public void trackLoaded(AudioTrack track) {
				BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " Ajout de *" + Utils.formatTrackName(track.getInfo()) + "* à la playlist.", context.getChannel());
				musicManager.getScheduler().queue(track);
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist) {
				for(AudioTrack track : playlist.getTracks()) {
					musicManager.getScheduler().queue(track);
				}
				BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " Toutes les musiques ont été ajoutés à la playlist, elle contient maintenant " + musicManager.getScheduler().getPlaylist().size() + " éléments.", context.getChannel());
				BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " Lecture en cours : *" + musicManager.getScheduler().getCurrentTrackName() + "*", context.getChannel());
			}

			@Override
			public void noMatches() {
				BotUtils.sendMessage(Emoji.WARNING + " Aucun résultat n'a été trouvé pour " + context.getArg(), context.getChannel());
			}

			@Override
			public void loadFailed(FriendlyException e) {
				Log.error("Le chargement de la musique a échoué.", e, context.getChannel());
			}
		});
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Aide pour la commande /" + context.getArg())
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(new Color(170, 196, 222))
				.appendDescription("**Joue la musique passée en URL.**")
				.appendField("Utilisation", "/play <url>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
