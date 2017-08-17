package me.shadorc.discordbot.command.music;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.music.TrackScheduler;
import me.shadorc.discordbot.utils.BotUtils;
import sx.blah.discord.util.EmbedBuilder;

public class StopCmd extends AbstractCommand {

	public StopCmd() {
		super(Role.USER, "stop");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		GuildMusicManager musicManager = GuildMusicManager.getGuildAudioPlayer(context.getGuild());

		TrackScheduler scheduler = musicManager.getScheduler();
		if(!scheduler.isPlaying()) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " No currently playing music.", context.getChannel());
			return;
		}

		musicManager.leaveVoiceChannel();
		BotUtils.sendMessage(Emoji.EXCLAMATION + " Music has been stopped by " + context.getAuthorName() + ".", context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Stop all the musics.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}