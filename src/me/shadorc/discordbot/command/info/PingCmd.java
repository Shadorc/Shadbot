package me.shadorc.discordbot.command.info;

import java.awt.Color;
import java.time.ZoneId;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;
import sx.blah.discord.util.EmbedBuilder;

public class PingCmd extends Command {

	public PingCmd() {
		super(false, "ping");
	}

	@Override
	public void execute(Context context) {
		long messageMillisTime = context.getMessage().getCreationDate().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		long currentMillisTime = System.currentTimeMillis();
		long ping = currentMillisTime - messageMillisTime;
		BotUtils.sendMessage(Emoji.GEAR + " Ping : " + ping + "ms", context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Aide pour la commande /" + context.getArg())
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(new Color(170, 196, 222))
				.appendDescription("**Affiche le ping de Shadbot.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
