package me.shadorc.discordbot.command.music;

import java.util.concurrent.TimeUnit;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.TextUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class ForwardCmd extends AbstractCommand {

	public ForwardCmd() {
		super(CommandCategory.MUSIC, Role.USER, RateLimiter.DEFAULT_COOLDOWN, "forward");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		GuildMusicManager musicManager = GuildMusicManager.getGuildMusicManager(context.getGuild());

		if(musicManager == null || musicManager.getScheduler().isStopped()) {
			BotUtils.sendMessage(TextUtils.NO_PLAYING_MUSIC, context.getChannel());
			return;
		}

		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		String numStr = context.getArg().trim();
		if(!StringUtils.isPositiveInt(numStr)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid number.", context.getChannel());
			return;
		}

		try {
			long time = TimeUnit.SECONDS.toMillis(Integer.parseInt(numStr));
			long newPosition = musicManager.getScheduler().changePosition(time);
			BotUtils.sendMessage(Emoji.CHECK_MARK + " New position: " + StringUtils.formatDuration(newPosition), context.getChannel());
		} catch (IllegalArgumentException err) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " New position is past the ending of the song.", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Fast forward current song a specified amount of time (in seconds).**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " <sec>`", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
