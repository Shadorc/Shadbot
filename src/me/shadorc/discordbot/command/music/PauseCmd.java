package me.shadorc.discordbot.command.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.TextUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class PauseCmd extends AbstractCommand {

	public PauseCmd() {
		super(CommandCategory.MUSIC, Role.USER, RateLimiter.DEFAULT_COOLDOWN, "pause", "unpause", "resume");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		GuildMusicManager musicManager = GuildMusicManager.getGuildMusicManager(context.getGuild());

		if(musicManager == null || musicManager.getScheduler().isStopped()) {
			BotUtils.sendMessage(TextUtils.NO_PLAYING_MUSIC, context.getChannel());
			return;
		}

		AudioPlayer audioPlayer = musicManager.getScheduler().getAudioPlayer();
		audioPlayer.setPaused(!audioPlayer.isPaused());
		if(audioPlayer.isPaused()) {
			BotUtils.sendMessage(Emoji.PAUSE + " Music paused by " + context.getAuthorName() + ".", context.getChannel());
		} else {
			BotUtils.sendMessage(Emoji.PLAY + " Music resumed by " + context.getAuthorName() + ".", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Pause current music. Use this command again to resume.**");
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}