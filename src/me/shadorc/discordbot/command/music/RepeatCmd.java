package me.shadorc.discordbot.command.music;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.music.TrackScheduler;
import me.shadorc.discordbot.utils.BotUtils;
import sx.blah.discord.util.EmbedBuilder;

public class RepeatCmd extends Command {

	public RepeatCmd() {
		super(false, "repeat");
	}

	@Override
	public void execute(Context context) {
		GuildMusicManager musicManager = GuildMusicManager.getGuildAudioPlayer(context.getGuild());
		TrackScheduler scheduler = musicManager.getScheduler();

		if(!scheduler.isPlaying()) {
			BotUtils.sendMessage(Emoji.WARNING + " No currently playing music.", context.getChannel());
			return;
		}

		scheduler.setRepeatEnabled(!scheduler.isRepeating());
		if(scheduler.isRepeating()) {
			BotUtils.sendMessage(Emoji.REPEAT + " Repetition enabled.", context.getChannel());
		} else {
			BotUtils.sendMessage(Emoji.PLAY + " Repetition disabled.", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for /" + this.getNames()[0])
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Enable/disable music repetition.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
