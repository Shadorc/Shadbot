package me.shadorc.discordbot.command.admin;

import java.util.List;
import java.util.stream.Collectors;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.EmbedBuilder;

public class AllowsChannelCmd extends Command {

	public AllowsChannelCmd() {
		super(true, "allows_channel");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		List<IChannel> channels = context.getMessage().getChannelMentions();
		if(channels.size() == 0) {
			throw new MissingArgumentException();
		}

		for(IChannel channel : channels) {
			if(!BotUtils.isChannelAllowed(context.getGuild(), channel)) {
				Storage.storePermission(context.getGuild(), channel);
			}
		}
		BotUtils.sendMessage(Emoji.CHECK_MARK + " Channel(s) " + channels.stream().map(channel -> channel.mention()).collect(Collectors.joining(", ")).trim() + " have been added to the authorized list.", context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for /" + this.getNames()[0])
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Allow Shadbot to only post messages in the mentioned channels.\nBy default, all the channels are allowed.**")
				.appendField("Usage", "/allows_channel <#channel>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
