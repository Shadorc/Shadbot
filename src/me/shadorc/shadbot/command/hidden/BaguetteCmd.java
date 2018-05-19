package me.shadorc.shadbot.command.hidden;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;

@RateLimited
@Command(category = CommandCategory.HIDDEN, names = { "baguette" })
public class BaguetteCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		EmbedCreateSpec embed = new EmbedCreateSpec()
				.setColor(Config.BOT_COLOR.getRGB())
				.setImage("http://i.telegraph.co.uk/multimedia/archive/02600/CECPY7_2600591b.jpg");
		BotUtils.sendMessage(embed, context.getChannel());
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("This command doesn't exist.")
				.build();
	}

}
