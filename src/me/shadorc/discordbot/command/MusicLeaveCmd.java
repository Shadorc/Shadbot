package me.shadorc.discordbot.command;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.music.GuildsMusicManager;
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
		GuildsMusicManager.getMusicManager(context.getGuild()).stop();
		botVoiceChannel.leave();
	}

}
