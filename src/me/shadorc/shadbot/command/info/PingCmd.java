package me.shadorc.shadbot.command.info;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DateUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;

@RateLimited
@Command(category = CommandCategory.INFO, names = { "ping" })
public class PingCmd extends AbstractCommand {

	@Override
	public void execute(Context context) {
		BotUtils.sendMessage(String.format(Emoji.GEAR + " Ping: %d ms",
				DateUtils.getMillisUntil(context.getMessage().getCreationDate())), context.getChannel());
	}

	@Override
	public EmbedObject getHelp(Context context) {
		return new HelpBuilder(this, context.getPrefix())
				.setDescription("Show Shadbot's ping.")
				.build();
	}
}
