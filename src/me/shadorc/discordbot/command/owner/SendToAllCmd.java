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
import sx.blah.discord.handle.obj.IGuild;
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

		for(IGuild guild : Shadbot.getClient().getGuilds()) {
			BotUtils.sendMessage(context.getArg().trim(), guild.getDefaultChannel());
		}

		BotUtils.sendMessage(Emoji.CHECK_MARK + " Message sent.", context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Send a message to all guilds.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " <message>`", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

}
