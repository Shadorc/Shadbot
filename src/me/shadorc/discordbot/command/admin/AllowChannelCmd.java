package me.shadorc.discordbot.command.admin;

import java.util.List;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.utility.BotUtils;
import sx.blah.discord.handle.obj.IChannel;

public class AllowChannelCmd extends Command {

	public AllowChannelCmd() {
		super(true, "allow_channel");
	}

	@Override
	public void execute(Context context) {
		if(context.getArg() == null) {
			BotUtils.sendMessage("Indiquez le nom du channel à autoriser.", context.getChannel());
			return;
		}

		if(context.getArg().equalsIgnoreCase("all")) {
			for(IChannel channel : context.getGuild().getChannels()) {
				Storage.store(context.getGuild(), "allowedChannels", channel.getStringID());
				BotUtils.sendMessage(channel.getName() + " a été ajouté à la liste des channels autorisés.", context.getChannel());
			}
		} else {
			List <IChannel> channelsByName = context.getGuild().getChannelsByName(context.getArg());
			if(channelsByName.size() == 0) {
				BotUtils.sendMessage("Aucun channel correspondant au nom " + context.getArg(), context.getChannel());
				return;
			}
			Storage.store(context.getGuild(), "allowedChannels", channelsByName.get(0).getStringID());
			BotUtils.sendMessage(channelsByName.get(0).getName() + " a été ajouté à la liste des channels autorisés.", context.getChannel());
		}
	}
}
