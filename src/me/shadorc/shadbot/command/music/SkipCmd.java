package me.shadorc.shadbot.command.music;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.CastUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;

@RateLimited(max = 1, cooldown = 1)
@Command(category = CommandCategory.MUSIC, names = { "skip", "next" })
public class SkipCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		GuildMusic guildMusic = GuildMusicManager.GUILD_MUSIC_MAP.get(context.getGuild().getLongID());

		if(guildMusic == null || guildMusic.getScheduler().isStopped()) {
			BotUtils.sendMessage(TextUtils.NO_PLAYING_MUSIC, context.getChannel());
			return;
		}

		if(context.hasArg()) {
			Integer num = CastUtils.asIntBetween(context.getArg(), 1, guildMusic.getScheduler().getPlaylist().size());
			if(num == null) {
				throw new IllegalCmdArgumentException(String.format("Number must be between 1 and %d.",
						guildMusic.getScheduler().getPlaylist().size()));
			}
			guildMusic.getScheduler().skipTo(num);
			return;
		}

		if(!guildMusic.getScheduler().nextTrack()) {
			guildMusic.end();
		}
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Skip current music and play the next one if it exists."
						+ "\nYou can also directly skip to a music in the playlist by specifying its number.")
				.addArg("num", "the number of the music in the playlist to play", true)
				.build();
	}
}