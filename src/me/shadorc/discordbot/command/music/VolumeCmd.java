package me.shadorc.discordbot.command.music;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.music.TrackScheduler;
import me.shadorc.discordbot.utils.BotUtils;
import sx.blah.discord.util.EmbedBuilder;

public class VolumeCmd extends Command {

	public VolumeCmd() {
		super(false, "volume");
	}

	@Override
	public void execute(Context context) {
		GuildMusicManager musicManager = GuildMusicManager.getGuildAudioPlayer(context.getGuild());
		TrackScheduler scheduler = musicManager.getScheduler();

		if(!scheduler.isPlaying()) {
			BotUtils.sendMessage(Emoji.WARNING + " No currently playing music.", context.getChannel());
			return;
		}

		if(context.getArg() == null) {
			BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " Current volume level: " + scheduler.getVolume() + "%", context.getChannel());
			return;
		}

		try {
			scheduler.setVolume(Integer.parseInt(context.getArg()));
			BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " Volume level set to " + scheduler.getVolume() + "%", context.getChannel());
		} catch (NumberFormatException e) {
			BotUtils.sendMessage(Emoji.WARNING + " Please use a value between 0 and 100.", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for /" + this.getNames()[0])
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show or change the current volume level.**")
				.appendField("Usage", "/volume or /volume <0-100>", false);

		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
