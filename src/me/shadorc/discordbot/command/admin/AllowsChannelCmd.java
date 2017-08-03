package me.shadorc.discordbot.command.admin;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.EmbedBuilder;

public class AllowsChannelCmd extends Command {

	public AllowsChannelCmd() {
		super(true, "allows_channel");
	}

	@Override
	public void execute(Context context) {
		List <IChannel> channels = context.getMessage().getChannelMentions();
		if(channels.size() == 0) {
			throw new IllegalArgumentException();
		}

		for(IChannel channel : channels) {
			if(!BotUtils.isChannelAllowed(context.getGuild(), channel)) {
				Storage.storePermission(context.getGuild(), channel);
			}
		}
		BotUtils.sendMessage(Emoji.CHECK_MARK + " Le(s) channel(s) " + channels.stream().map(channel -> channel.mention()).collect(Collectors.joining(", ")).trim() + " a/ont été ajouté(s) à la liste des channels autorisés.", context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Aide pour la commande /" + context.getArg())
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(new Color(170, 196, 222))
				.appendDescription("**Autorise Shadbot à poster des messages uniquement dans les channels mentionnés.\nPar défaut, tous les channels sont autorisés.**")
				.appendField("Utilisation", "/allows_channel <#channel>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
