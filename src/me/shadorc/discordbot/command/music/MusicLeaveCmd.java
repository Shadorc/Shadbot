package me.shadorc.discordbot.command.music;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utility.BotUtils;
import sx.blah.discord.handle.obj.IVoiceChannel;

public class MusicLeaveCmd extends Command {

	public MusicLeaveCmd() {
		super("leave", "quit");
	}

	@Override
	public void execute(Context context) {
		IVoiceChannel botVoiceChannel = context.getClient().getOurUser().getVoiceStateForGuild(context.getGuild()).getChannel();
		if(botVoiceChannel == null) {
			BotUtils.sendMessage("Je ne suis dans aucun salon vocal.", context.getChannel());
			return;
		}
		GuildMusicManager.getGuildAudioPlayer(context.getGuild()).getScheduler().stop();
		botVoiceChannel.leave();
	}
}
