package me.shadorc.discordbot.command;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.utility.BotUtils;
import sx.blah.discord.handle.obj.IVoiceChannel;

public class MusicJoinCommand extends Command {

	public MusicJoinCommand() {
		super("join", "rejoindre", "joindre");
	}

	@Override
	public void execute(Context context) {
		IVoiceChannel userVoiceChannel = context.getAuthor().getVoiceStateForGuild(context.getGuild()).getChannel();
		if(userVoiceChannel == null) {
			BotUtils.sendMessage("Rejoignez un salon vocal avant d'utiliser cette commande.", context.getChannel());
			return;
		}
		userVoiceChannel.join();
	}

}
