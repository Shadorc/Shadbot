package me.shadorc.discordbot.command.admin;

import java.util.stream.Collectors;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.NetUtils;
import sx.blah.discord.util.EmbedBuilder;

public class DebugCmd extends Command {

	public DebugCmd() {
		super(true, "debug");
	}

	@Override
	public void execute(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Debug Info")
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendField("Ping", Integer.toString(NetUtils.getPing()), true)
				.appendField("Guild ID", context.getGuild().getStringID(), true)
				.appendField("Members count", Integer.toString(context.getGuild().getTotalMemberCount()), true)
				.appendField("Channel(s)", context.getGuild().getChannels().stream().map(
						channel -> channel.mention()
								+ " | ID: " + channel.getStringID()
								+ " | Authorized : " + BotUtils.isChannelAllowed(context.getGuild(), channel))
						.collect(Collectors.joining("\n")), true);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for /" + this.getNames()[0])
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show debug info.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
