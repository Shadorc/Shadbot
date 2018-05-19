package me.shadorc.shadbot.command.music;

import java.util.concurrent.TimeUnit;

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
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;

@RateLimited
@Command(category = CommandCategory.MUSIC, names = { "backward" })
public class BackwardCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		GuildMusic guildMusic = GuildMusicManager.GUILD_MUSIC_MAP.get(context.getGuild().getLongID());

		if(guildMusic == null || guildMusic.getScheduler().isStopped()) {
			BotUtils.sendMessage(TextUtils.NO_PLAYING_MUSIC, context.getChannel());
			return;
		}

		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		Long num = NumberUtils.asPositiveLong(context.getArg());
		if(num == null) {
			try {
				num = TimeUtils.parseTime(context.getArg());
			} catch (IllegalArgumentException err) {
				throw new IllegalCmdArgumentException(String.format("`%s` is not a valid number / time.", context.getArg()));
			}
		}

		long newPosition = guildMusic.getScheduler().changePosition(-TimeUnit.SECONDS.toMillis(num));
		BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " New position: **%s**", FormatUtils.formatShortDuration(newPosition)),
				context.getChannel());
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Fast backward current song a specified amount of time.")
				.addArg("time", "can be seconds or time (e.g. 1m12s)", false)
				.build();
	}
}
