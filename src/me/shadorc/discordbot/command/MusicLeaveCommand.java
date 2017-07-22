package me.shadorc.discordbot.command;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.utility.BotUtils;
import sx.blah.discord.handle.obj.IVoiceChannel;

public class MusicLeaveCommand extends Command {

	public MusicLeaveCommand() {
		super("leave", "quit");
	}

	@Override
	public void execute(Context context) {
		IVoiceChannel botVoiceChannel = context.getClient().getOurUser().getVoiceStateForGuild(context.getGuild()).getChannel();
		if(botVoiceChannel == null) {
			BotUtils.sendMessage("Je ne suis dans aucun salon vocal.", context.getChannel());
			return;
		}
		botVoiceChannel.leave();
	}

}
