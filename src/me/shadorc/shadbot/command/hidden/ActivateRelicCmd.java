package me.shadorc.shadbot.command.hidden;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.data.premium.PremiumManager;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.exception.RelicActivationException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;

@Command(category = CommandCategory.HIDDEN, names = { "activate_relic", "activate" })
public class ActivateRelicCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		context.requireArg();

		try {
			PremiumManager.activateRelic(context.getGuild().get(), context.getAuthor(), context.getArg().get());
			BotUtils.sendMessage(Emoji.CHECK_MARK + " Relic successfully activated, enjoy !", context.getChannel());
		} catch (RelicActivationException err) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " " + err.getMessage(), context.getChannel());
		}
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Activate a relic.")
				.addArg("key", false)
				.build();
	}
}
