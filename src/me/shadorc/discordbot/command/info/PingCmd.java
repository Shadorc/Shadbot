package me.shadorc.discordbot.command.info;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.NetUtils;
import sx.blah.discord.util.EmbedBuilder;

public class PingCmd extends Command {

	public PingCmd() {
		super(false, "ping");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		long ping = NetUtils.getPing();
		BotUtils.sendMessage(Emoji.GEAR + " Ping : " + ping + "ms", context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show Shadbot's ping.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
