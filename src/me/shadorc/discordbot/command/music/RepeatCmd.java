package me.shadorc.discordbot.command.music;

import java.time.temporal.ChronoUnit;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.music.TrackScheduler;
import me.shadorc.discordbot.music.TrackScheduler.RepeatMode;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.TextUtils;
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
			BotUtils.sendMessage(TextUtils.NO_PLAYING_MUSIC, context.getChannel());
			return;
		}

		if(context.hasArg() && !context.getArg().equalsIgnoreCase(RepeatMode.PLAYLIST.toString())) {
			throw new MissingArgumentException();
		}

		TrackScheduler scheduler = musicManager.getScheduler();

		if(!context.hasArg()) {
			if(scheduler.getRepeatMode() == RepeatMode.SONG) {
				scheduler.setRepeatMode(RepeatMode.NONE);
				BotUtils.sendMessage(Emoji.PLAY + " Repetition disabled.", context.getChannel());
			} else {
				scheduler.setRepeatMode(RepeatMode.SONG);
				BotUtils.sendMessage(Emoji.REPEAT + " Repetition enabled.", context.getChannel());
			}
			return;
		}

		if(context.getArg().equalsIgnoreCase(RepeatMode.PLAYLIST.toString())) {
			if(scheduler.getRepeatMode() == RepeatMode.PLAYLIST) {
				scheduler.setRepeatMode(RepeatMode.NONE);
				BotUtils.sendMessage(Emoji.PLAY + " Playlist repetition disabled.", context.getChannel());
			} else {
				scheduler.setRepeatMode(RepeatMode.PLAYLIST);
				BotUtils.sendMessage(Emoji.REPEAT + " Playlist repetition enabled.", context.getChannel());
			}
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Enable/disable song/playlist repetition.**")
				.appendField("Usage", "`" + context.getPrefix() + "repeat [playlist]`", false)
				.appendField("Argument", "**playlist** - [OPTIONAL] repeat the current playlist", false)
				.appendField("Info", "Reuse this command to toggle repetition", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

}
