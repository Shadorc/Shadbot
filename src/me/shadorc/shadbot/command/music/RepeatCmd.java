package me.shadorc.shadbot.command.music;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.music.RepeatMode;
import me.shadorc.shadbot.music.TrackScheduler;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;

@RateLimited
@Command(category = CommandCategory.MUSIC, names = { "repeat", "loop" })
public class RepeatCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		GuildMusic guildMusic = GuildMusicManager.GUILD_MUSIC_MAP.get(context.getGuild().getLongID());

		if(guildMusic == null || guildMusic.getScheduler().isStopped()) {
			BotUtils.sendMessage(TextUtils.NO_PLAYING_MUSIC, context.getChannel());
			return;
		}

		RepeatMode mode = Utils.getValueOrNull(RepeatMode.class, context.getArg());

		if(context.hasArg() && !RepeatMode.PLAYLIST.equals(mode)) {
			throw new MissingArgumentException();
		}

		// By default, modification are made on song repeat mode
		if(mode == null) {
			mode = RepeatMode.SONG;
		}

		TrackScheduler scheduler = guildMusic.getScheduler();

		scheduler.setRepeatMode(scheduler.getRepeatMode().equals(mode) ? RepeatMode.NONE : mode);
		BotUtils.sendMessage(String.format("%s %sRepetition %s",
				scheduler.getRepeatMode().equals(RepeatMode.NONE) ? Emoji.PLAY : Emoji.REPEAT,
				RepeatMode.PLAYLIST.equals(mode) ? "Playlist " : "",
				scheduler.getRepeatMode().equals(RepeatMode.NONE) ? "disabled" : "enabled"), context.getChannel());
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Toggle song/playlist repetition.")
				.setUsage("[playlist]")
				.addArg("playlist", "repeat the current playlist", true)
				.build();
	}

}
