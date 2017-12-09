package me.shadorc.discordbot.command.owner;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;

public class SendToAllCmd extends AbstractCommand {

	public SendToAllCmd() {
		super(CommandCategory.OWNER, Role.OWNER, "send_to_all");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		int count = 0;
		for(IGuild guild : Shadbot.getClient().getGuilds()) {
			if(BotUtils.isChannelAllowed(guild, guild.getDefaultChannel()) && BotUtils.hasPermission(guild.getDefaultChannel(), Permissions.SEND_MESSAGES)) {
				BotUtils.sendMessage(context.getArg().trim(), guild.getDefaultChannel());
				count++;
			} else {
				for(IChannel channel : guild.getChannels()) {
					if(BotUtils.isChannelAllowed(guild, channel) && BotUtils.hasPermission(channel, Permissions.SEND_MESSAGES)) {
						BotUtils.sendMessage(context.getArg().trim(), channel);
						count++;
						break;
					}
				}
			}
		}

		BotUtils.sendMessage(Emoji.CHECK_MARK + " Message sent to " + count + "/" + Shadbot.getClient().getGuilds().size() + " guilds.", context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Send a message to all guilds.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " <message>`", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
