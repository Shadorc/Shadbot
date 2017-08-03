package me.shadorc.discordbot.command.rpg;

import java.awt.Color;

import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;
import sx.blah.discord.util.EmbedBuilder;

public class InfoCmd extends Command {

	public InfoCmd() {
		super(false, "info", "infos");
	}

	@Override
	public void execute(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("RPG Stats")
				.withAuthorIcon(context.getAuthor().getAvatarURL())
				.withThumbnail("http://image.flaticon.com/icons/png/512/297/297806.png")
				.withColor(new Color(170, 196, 222))
				.withDesc("Informations sur le personnage de " + context.getAuthorName() + ".")
				.appendField("Niveau", Integer.toString(context.getUser().getLevel()), true)
				.appendField("Vie", Integer.toString(context.getUser().getLife()), true)
				.appendField("Coins", Integer.toString(context.getUser().getCoins()), true)
				.appendField("XP", context.getUser().getXp() + "/" + context.getUser().getXpToNextLevel(), true);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Aide pour la commande /" + context.getArg())
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(new Color(170, 196, 222))
				.appendDescription("**Affiche votre niveaue.\nPour connaître le niveau d'un autre utilisateur, mentionnez le.**")
				.appendField("Utilisation", "/level ou /level <@user>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
