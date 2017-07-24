package me.shadorc.discordbot.command.admin;

import java.util.List;
import java.util.stream.Collectors;

import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Utils;
import sx.blah.discord.handle.obj.IChannel;

public class AllowsChannelCmd extends Command {

	public AllowsChannelCmd() {
		super(true, "allows_channel");
	}

	@Override
	public void execute(Context context) {
		if(context.getArg() == null) {
			BotUtils.sendMessage("Mentionnez un ou plusieurs channels à autoriser.", context.getChannel());
			return;
		}

		if(context.getArg().equalsIgnoreCase("all")) {
			this.addChannels(context, context.getGuild().getChannels());
		} else {
			List <IChannel> channels = context.getMessage().getChannelMentions();
			if(channels.size() == 0) {
				BotUtils.sendMessage("Vous devez mentionner au moins un channel.", context.getChannel());
				return;
			}
			this.addChannels(context, channels);
		}
	}

	private void addChannels(Context context, List <IChannel> channels) {
		for(IChannel channel : channels) {
			if(!Utils.isChannelAllowed(context.getGuild(), channel)) {
				Storage.store(context.getGuild(), "allowedChannels", channel.getStringID());
			}
		}
		BotUtils.sendMessage("Le(s) channel(s) *" + channels.stream().map(channel -> channel.getName()).collect(Collectors.joining(", ")).trim() + "* a/ont été ajouté(s) à la liste des channels autorisés.", context.getChannel());
	}
}
