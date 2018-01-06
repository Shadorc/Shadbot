package me.shadorc.shadbot.command.music;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.music.TrackScheduler;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.CastUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.api.internal.json.objects.EmbedObject;

@RateLimited
@Command(category = CommandCategory.MUSIC, names = { "volume" }, alias = "vol")
public class VolumeCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		GuildMusic guildMusic = GuildMusicManager.GUILD_MUSIC_MAP.get(context.getGuild().getLongID());

		if(guildMusic == null || guildMusic.getScheduler().isStopped()) {
			BotUtils.sendMessage(TextUtils.NO_PLAYING_MUSIC, context.getChannel());
			return;
		}

		TrackScheduler scheduler = guildMusic.getScheduler();
		if(!context.hasArg()) {
			BotUtils.sendMessage(String.format(Emoji.SOUND + " Current volume level: %d%%", scheduler.getAudioPlayer().getVolume()),
					context.getChannel());
			return;
		}

		Integer volume = CastUtils.asPositiveInt(context.getArg());
		if(volume == null) {
			throw new IllegalArgumentException("Invalid volume.");
		}

		scheduler.setVolume(volume);
		BotUtils.sendMessage(String.format(Emoji.SOUND + " Volume level set to **%s%%**", scheduler.getAudioPlayer().getVolume()),
				context.getChannel());
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show or change current volume level.")
				.addArg("volume", "must be between 0 and 100", true)
				.build();
	}
}
