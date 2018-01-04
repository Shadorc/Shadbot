package me.shadorc.shadbot.command.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;

@RateLimited
@Command(category = CommandCategory.MUSIC, names = { "pause", "unpause", "resume" })
public class PauseCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		GuildMusic guildMusic = GuildMusicManager.GUILD_MUSIC_MAP.get(context.getGuild().getLongID());

		if(guildMusic == null || guildMusic.getScheduler().isStopped()) {
			BotUtils.sendMessage(TextUtils.NO_PLAYING_MUSIC, context.getChannel());
			return;
		}

		AudioPlayer audioPlayer = guildMusic.getScheduler().getAudioPlayer();
		audioPlayer.setPaused(!audioPlayer.isPaused());
		if(audioPlayer.isPaused()) {
			BotUtils.sendMessage(String.format(Emoji.PAUSE + " Music paused by **%s**.", context.getAuthorName()), context.getChannel());
		} else {
			BotUtils.sendMessage(String.format(Emoji.PLAY + " Music resumed by **%s**.", context.getAuthorName()), context.getChannel());
		}
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Pause current music. Use this command again to resume.")
				.build();
	}
}