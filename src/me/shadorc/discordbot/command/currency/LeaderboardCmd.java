package me.shadorc.discordbot.command.currency;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Utils;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class LeaderboardCmd extends Command {

	public LeaderboardCmd() {
		super(false, "leaderboard", "classement");
	}

	@Override
	public void execute(Context context) {
		Map <IUser, Integer> usersCoin = new HashMap<>();
		for(IUser user : context.getGuild().getUsers()) {
			int userCoin = Storage.getCoins(context.getGuild(), user);
			if(userCoin > 0) {
				usersCoin.put(user, userCoin);
			}
		}
		usersCoin = Utils.sortByValue(usersCoin);

		int count = 1;
		StringBuilder strBuilder = new StringBuilder();
		for(IUser user : usersCoin.keySet()) {
			if(count > 10) {
				break;
			}
			strBuilder.append("\n" + count + ". **" + user.getName() + "** - " + usersCoin.get(user) + " coins");
			count++;
		}

		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Classement")
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(new Color(170, 196, 222))
				.withDescription(strBuilder.toString());

		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Aide pour la commande /" + context.getArg())
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(new Color(170, 196, 222))
				.appendDescription("**Affiche le classement des coins du serveur.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
