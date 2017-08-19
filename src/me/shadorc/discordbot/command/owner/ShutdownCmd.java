package me.shadorc.discordbot.command.owner;

import java.util.Timer;
import java.util.TimerTask;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.StringUtils;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.util.EmbedBuilder;

public class ShutdownCmd extends AbstractCommand {

	public ShutdownCmd() {
		super(Role.OWNER, "shutdown");
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
		if(!StringUtils.isInteger(timeStr)) {
			throw new MissingArgumentException();
		}

		int time = Integer.parseInt(timeStr);
		String message = splitArgs[1].trim();

		for(IGuild guild : Shadbot.getClient().getGuilds()) {
			if(Shadbot.getClient().getOurUser().getVoiceStateForGuild(guild).getChannel() != null) {
				BotUtils.sendMessage(Emoji.INFO + " " + message, GuildMusicManager.getGuildAudioPlayer(guild).getChannel());
			}
		}

		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				Shadbot.getClient().logout();
			}
		};
		new Timer().schedule(task, time * 1000);

		LogUtils.warn("Shadbot will restart in " + time + " seconds with the message: " + message);
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Schedules a fixed amount of seconds the bot will wait to be shutted down and send a message to all guilds.**")
				.appendField("Usage", context.getPrefix() + "shutdown <seconds> <message>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
