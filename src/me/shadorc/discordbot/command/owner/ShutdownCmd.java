package me.shadorc.discordbot.command.owner;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import me.shadorc.discordbot.utils.schedule.Scheduler;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.util.EmbedBuilder;

public class ShutdownCmd extends AbstractCommand {

	protected final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	public ShutdownCmd() {
		super(CommandCategory.OWNER, Role.OWNER, RateLimiter.DEFAULT_COOLDOWN, "shutdown");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		Runnable shutdownTask = new Runnable() {
			@Override
			public void run() {
				Shadbot.getClient().logout();
				Scheduler.stop();
				Shadbot.getDefaultThreadPool().shutdownNow();
				executor.shutdownNow();
				System.exit(0);
			}
		};

		if(!context.hasArg()) {
			executor.submit(shutdownTask);
			return;
		}

		String[] splitArgs = StringUtils.getSplittedArg(context.getArg(), 2);
		if(splitArgs.length != 2) {
			throw new MissingArgumentException();
		}

		String timeStr = splitArgs[0];
		if(!StringUtils.isPositiveInt(timeStr)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid time.", context.getChannel());
			return;
		}

		String message = splitArgs[1].trim();
		for(IGuild guild : Shadbot.getClient().getGuilds()) {
			GuildMusicManager musicManager = GuildMusicManager.getGuildMusicManager(guild);
			if(musicManager != null && musicManager.getChannel() != null) {
				BotUtils.sendMessage(Emoji.INFO + " " + message, musicManager.getChannel());
			}
		}

		int delay = Integer.parseInt(timeStr);
		executor.schedule(shutdownTask, delay, TimeUnit.SECONDS);

		LogUtils.warn("Shadbot will restart in " + delay + " seconds. (Message: " + message + ")");
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Schedule a shutdown after a fixed amount of seconds and send a message to all guilds playing musics.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " [<seconds> <message>]`", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

}
