package me.shadorc.discordbot.command.music;

import java.time.temporal.ChronoUnit;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.music.TrackScheduler;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class RepeatCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public RepeatCmd() {
		super(CommandCategory.MUSIC, Role.USER, "repeat", "loop");
		this.rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		GuildMusicManager musicManager = GuildMusicManager.getGuildMusicManager(context.getGuild());

		if(musicManager == null || musicManager.getScheduler().isStopped()) {
			BotUtils.sendMessage(Emoji.MUTE + " No currently playing music.", context.getChannel());
			return;
		}

		TrackScheduler scheduler = musicManager.getScheduler();
		scheduler.setRepeat(!scheduler.isRepeating());
		if(scheduler.isRepeating()) {
			BotUtils.sendMessage(Emoji.REPEAT + " Repetition enabled.", context.getChannel());
		} else {
			BotUtils.sendMessage(Emoji.PLAY + " Repetition disabled.", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Enable/disable music repetition.**");
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

}
