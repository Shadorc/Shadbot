package me.shadorc.discordbot.command.music;

import java.util.concurrent.BlockingQueue;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.StringUtils;
import sx.blah.discord.util.EmbedBuilder;

public class PlaylistCmd extends AbstractCommand {

	public PlaylistCmd() {
		super(Role.USER, "playlist");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		GuildMusicManager musicManager = GuildMusicManager.getGuildMusicManager(context.getGuild());

		if(musicManager == null) {
			BotUtils.sendMessage(Emoji.MUTE + " No currently playing music.", context.getChannel());
			return;
		}

		EmbedBuilder embed = new EmbedBuilder()
				.withAuthorName("Playlist")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withThumbnail("http://icons.iconarchive.com/icons/dtafalonso/yosemite-flat/512/Music-icon.png")
				.withDescription(this.formatPlaylist(musicManager.getScheduler().getPlaylist()));
		BotUtils.sendEmbed(embed.build(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show the current playlist.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

	private String formatPlaylist(BlockingQueue<AudioTrack> queue) {
		StringBuilder playlist = new StringBuilder("**" + queue.size() + " music(s) in the playlist:**\n");

		int count = 1;
		for(AudioTrack track : queue) {
			String name = "\n\t**" + count + ".** " + StringUtils.formatTrackName(track.getInfo());
			if(playlist.length() + name.length() < 1800) {
				playlist.append(name);
				count++;
			} else {
				playlist.append("\n\t...");
				break;
			}
		}
		return playlist.toString();
	}
}