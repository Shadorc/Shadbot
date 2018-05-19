package me.shadorc.shadbot.command.info;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;

@RateLimited
@Command(category = CommandCategory.INFO, names = { "ping" })
public class PingCmd extends AbstractCommand {

	@Override
	public void execute(Context context) {
		BotUtils.sendMessage(String.format(Emoji.GEAR + " Ping: %dms",
				TimeUtils.getMillisUntil(context.getMessage().getTimestamp())), context.getChannel());
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show Shadbot's ping.")
				.build();
	}
}
