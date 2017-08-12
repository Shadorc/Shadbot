package me.shadorc.discordbot.command.rpg;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import sx.blah.discord.util.EmbedBuilder;

public class CharacterCmd extends Command {

	public CharacterCmd() {
		super(false, "character", "personnage");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("RPG Stats")
				.withAuthorIcon(context.getAuthor().getAvatarURL())
				.withThumbnail("http://image.flaticon.com/icons/png/512/297/297806.png")
				.withColor(Config.BOT_COLOR)
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
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Affiche votre niveaue.\nPour connaître le niveau d'un autre utilisateur, mentionnez le.**")
				.appendField("Utilisation", "/level ou /level <@user>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
