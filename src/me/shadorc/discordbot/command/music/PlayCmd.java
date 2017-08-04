package me.shadorc.discordbot.command.music;

import java.awt.Color;
import java.util.List;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.Log;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.StringUtils;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.EmbedBuilder;

public class PlayCmd extends Command {

	public PlayCmd() {
		super(false, "play", "joue");
	}

	@Override
	public void execute(Context context) {
		if(context.getArg() == null) {
			BotUtils.sendMessage(Emoji.WARNING + " Merci d'entrer l'URL ou le nom d'une musique à écouter.", context.getChannel());
			return;
		}

		final StringBuilder identifier = new StringBuilder();
		if(NetUtils.isValidURL(context.getArg())) {
			identifier.append(context.getArg());
		} else {
			identifier.append("ytsearch: " + context.getArg());
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

		GuildMusicManager musicManager = GuildMusicManager.getGuildAudioPlayer(context.getGuild());
		musicManager.setChannel(context.getChannel());

		GuildMusicManager.PLAYER_MANAGER.loadItemOrdered(musicManager, identifier.toString(), new AudioLoadResultHandler() {
			@Override
			public void trackLoaded(AudioTrack track) {
				if(musicManager.getScheduler().isPlaying()) {
					BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " Ajout de **" + StringUtils.formatTrackName(track.getInfo()) + "** à la playlist.", context.getChannel());
				}
				musicManager.getScheduler().queue(track);
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist) {
				List<AudioTrack> tracks = playlist.getTracks();

				if(identifier.toString().startsWith("ytsearch: ")) {
					if(musicManager.getScheduler().isPlaying()) {
						BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " Ajout de **" + StringUtils.formatTrackName(tracks.get(0).getInfo()) + "** à la playlist.", context.getChannel());
					}
					musicManager.getScheduler().queue(tracks.get(0));
					return;
				}

				for(int i = 0; i < Math.min(200, tracks.size()); i++) {
					AudioTrack track = tracks.get(i);
					musicManager.getScheduler().queue(track);
				}
				BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " Toutes les musiques ont été ajoutés à la playlist, elle contient maintenant " + musicManager.getScheduler().getPlaylist().size() + " éléments.", context.getChannel());
			}

			@Override
			public void noMatches() {
				BotUtils.sendMessage(Emoji.WARNING + " Aucun résultat n'a été trouvé pour \"" + identifier.toString() + "\"", context.getChannel());
				GuildMusicManager.getGuildAudioPlayer(context.getGuild()).leave();
			}

			@Override
			public void loadFailed(FriendlyException e) {
				Log.error("Le chargement de la musique a échoué.", e, context.getChannel());
				GuildMusicManager.getGuildAudioPlayer(context.getGuild()).leave();
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
