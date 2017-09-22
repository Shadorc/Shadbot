package me.shadorc.discordbot.command.owner;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.SchedulerManager;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.util.EmbedBuilder;

public class ShutdownCmd extends AbstractCommand {

	public ShutdownCmd() {
		super(CommandCategory.OWNER, Role.OWNER, "shutdown");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		String[] splitArgs = context.getArg().split(" ", 2);
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
			GuildMusicManager gmm = GuildMusicManager.getGuildMusicManager(guild);
			if(gmm != null && gmm.getChannel() != null) {
				BotUtils.sendMessage(Emoji.INFO + " " + message, gmm.getChannel());
			}
		}

		Runnable shutdownTask = new Runnable() {
			@Override
			public void run() {
				Shadbot.getClient().logout();
				SchedulerManager.forceExecution();
			}
		};

		int delay = Integer.parseInt(timeStr);
		Executors.newSingleThreadScheduledExecutor().schedule(shutdownTask, delay, TimeUnit.SECONDS);

		LogUtils.warn("Shadbot will restart in " + delay + " seconds. (Message: " + message + ")");
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Schedule a shutdown after a fixed amount of seconds and send a message to all guilds playing musics.**")
				.appendField("Usage", "`" + context.getPrefix() + "shutdown <seconds> <message>`", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
