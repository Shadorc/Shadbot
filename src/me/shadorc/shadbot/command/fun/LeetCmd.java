package me.shadorc.shadbot.command.fun;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;

@RateLimited
@Command(category = CommandCategory.FUN, names = { "leet" })
public class LeetCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		context.requireArg();

		String text = context.getArg()
				.get()
				.toUpperCase()
				.replace("A", "4")
				.replace("B", "8")
				.replace("E", "3")
				.replace("G", "6")
				.replace("L", "1")
				.replace("O", "0")
				.replace("S", "5")
				.replace("T", "7");

		BotUtils.sendMessage(Emoji.KEYBOARD + " " + text, context.getChannel());
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Leetify a text.")
				.addArg("text", false)
				.build();
	}

}
