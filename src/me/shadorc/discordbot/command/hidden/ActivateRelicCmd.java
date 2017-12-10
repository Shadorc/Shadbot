package me.shadorc.discordbot.command.hidden;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.PremiumManager;
import me.shadorc.discordbot.exceptions.MissingArgumentException;
import me.shadorc.discordbot.exceptions.RelicActivationException;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import sx.blah.discord.util.EmbedBuilder;

public class ActivateRelicCmd extends AbstractCommand {

	public ActivateRelicCmd() {
		super(CommandCategory.HIDDEN, Role.USER, "activate_relic");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}
		try {
			PremiumManager.activateRelic(context.getGuild(), context.getAuthor(), context.getArg());
			BotUtils.sendMessage(Emoji.CHECK_MARK + " Relic successfully activated, enjoy !", context.getChannel());
		} catch (RelicActivationException err) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " " + err.getMessage(), context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Activate a relic.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " <key>`", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
