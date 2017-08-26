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

public class VolumeCmd extends AbstractCommand {

	public VolumeCmd() {
		super(Role.USER, "volume", "vol");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		GuildMusicManager musicManager = GuildMusicManager.getGuildMusicManager(context.getGuild());

		if(musicManager == null || musicManager.getScheduler().isStopped()) {
			BotUtils.sendMessage(Emoji.MUTE + " No currently playing music.", context.getChannel());
			return;
		}

		TrackScheduler scheduler = musicManager.getScheduler();
		if(!context.hasArg()) {
			BotUtils.sendMessage(Emoji.SPEAKER + " Current volume level: " + scheduler.getVolume() + "%", context.getChannel());
			return;
		}

		try {
			scheduler.setVolume(Integer.parseInt(context.getArg()));
			BotUtils.sendMessage(Emoji.SPEAKER + " Volume level set to " + scheduler.getVolume() + "%", context.getChannel());
		} catch (NumberFormatException err) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " Please use a value between 0 and 100.", context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show or change the current volume level.**")
				.appendField("Usage", context.getPrefix() + "volume or " + context.getPrefix() + "volume <0-100>", false);

		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
