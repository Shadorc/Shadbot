package me.shadorc.discordbot.command.music;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.music.TrackScheduler;
import me.shadorc.discordbot.utils.BotUtils;
import sx.blah.discord.util.EmbedBuilder;

public class PauseCmd extends Command {

	public PauseCmd() {
		super(false, "pause");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		GuildMusicManager musicManager = GuildMusicManager.getGuildAudioPlayer(context.getGuild());
		TrackScheduler scheduler = musicManager.getScheduler();

		if(!scheduler.isPlaying()) {
			BotUtils.sendMessage(Emoji.WARNING + " No currently playing music.", context.getChannel());
			return;
		}

		scheduler.setPaused(!scheduler.isPaused());
		if(scheduler.isPaused()) {
			BotUtils.sendMessage(Emoji.PAUSE + " Music paused by " + context.getAuthorName() + ".", context.getChannel());
		} else {
			BotUtils.sendMessage(Emoji.PLAY + " Music resumed by " + context.getAuthorName() + ".", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for /" + this.getNames()[0])
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Pause the current playing music. Use this command again to resume.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}