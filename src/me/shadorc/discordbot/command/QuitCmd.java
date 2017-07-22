package me.shadorc.discordbot.command;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IVoiceChannel;

public class QuitCmd extends Command {

	public QuitCmd() {
		super("quit");
	}

	@Override
	public void execute(Context context) {
		for(IGuild guild : context.getClient().getGuilds()) {
			GuildMusicManager.getGuildAudioPlayer(guild).getScheduler().stop();
			IVoiceChannel botVoiceChannel = context.getClient().getOurUser().getVoiceStateForGuild(guild).getChannel();
			if(botVoiceChannel != null) {
				botVoiceChannel.leave();
			}
		}
		System.exit(0);
	}
}