package me.shadorc.discordbot.command.admin;

import me.shadorc.discordbot.Main;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utility.BotUtils;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IVoiceChannel;

public class QuitCmd extends Command {

	public QuitCmd() {
		super(true, "quit");
	}

	@Override
	public void execute(Context context) {
		BotUtils.sendMessage("Arrêt en cours...", context.getChannel());
		for(IGuild guild : Main.getClient().getGuilds()) {
			GuildMusicManager guildMusicManager = GuildMusicManager.getGuildAudioPlayer(guild);
			if(guildMusicManager != null) {
				guildMusicManager.getScheduler().stop();
				IVoiceChannel botVoiceChannel = Main.getClient().getOurUser().getVoiceStateForGuild(guild).getChannel();
				if(botVoiceChannel != null) {
					botVoiceChannel.leave();
				}
			}
		}
		System.exit(0);
	}
}
