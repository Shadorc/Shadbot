package me.shadorc.discordbot.command.game;

import java.awt.Color;
import java.util.HashMap;

import javax.swing.Timer;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class DiceCmd extends Command {

	private static final HashMap <IGuild, DiceManager> GUILDS_DICE = new HashMap<>();
	private static final int MULTIPLIER = 6;

	public DiceCmd() {
		super(false, "des", "dice");
	}

	@Override
	public void execute(Context context) {
		if(!GUILDS_DICE.containsKey(context.getGuild())) {
			if(context.getArg() == null) {
				throw new IllegalArgumentException();
			}

			String[] splitArgs = context.getArg().split(" ");
			if(splitArgs.length != 2 || !Utils.isInteger(splitArgs[0]) || !Utils.isInteger(splitArgs[1])) {
				throw new IllegalArgumentException();
			}

			int bet = Integer.parseInt(splitArgs[0]);
			int num = Integer.parseInt(splitArgs[1]);

			if(num < 1 || num > 6) {
				throw new IllegalArgumentException();
			}

			if(context.getUser().getCoins() < bet) {
				BotUtils.sendMessage(Emoji.BANK + " Vous n'avez pas assez de coins pour effectuer cette mise.", context.getChannel());
				return;
			}

			GUILDS_DICE.put(context.getGuild(), new DiceManager(context.getChannel(), context.getAuthor(), num, bet));
		}

		else {
			DiceManager diceManager = GUILDS_DICE.get(context.getGuild());

			if(context.getUser().getCoins() < diceManager.getBet()) {
				BotUtils.sendMessage(Emoji.BANK + " Vous n'avez pas assez de coins pour rejoindre cette partie.", context.getChannel());
				return;
			}

			if(diceManager.isAlreadyPlaye(context.getAuthor())) {
				BotUtils.sendMessage(Emoji.WARNING + " " + context.getAuthor().mention() + ", tu participes déjà.", context.getChannel());
				return;
			}

			if(context.getArg() == null || !Utils.isInteger(context.getArg())) {
				throw new IllegalArgumentException();
			}

			int num = Integer.parseInt(context.getArg());

			if(num < 1 || num > 6) {
				throw new IllegalArgumentException();
			}

			if(diceManager.isAlreadyBet(num)) {
				BotUtils.sendMessage(Emoji.WARNING + " Ce numéro a déjà été misé, merci d'en indiquer un autre.", context.getChannel());
				return;
			}

			diceManager.addPlayer(context.getAuthor(), num);
			BotUtils.sendMessage(Emoji.DICE + " " + context.getAuthor().mention() + " a misé sur le " + num + ".", context.getChannel());
		}
	}

	private class DiceManager {

		private HashMap<Integer, IUser> numsPlayers;
		private IChannel channel;
		private IUser croupier;
		private Timer timer;
		private int bet;

		private DiceManager(IChannel channel, IUser user, int num, int bet) {
			this.channel = channel;
			this.croupier = user;
			this.numsPlayers = new HashMap<>();
			this.numsPlayers.put(num, user);
			this.bet = bet;
			this.timer = new Timer(30*1000, e -> {
				this.stop();
			});
			this.start();
		}

		private void addPlayer(IUser user, int num) {
			this.numsPlayers.put(num, user);
		}

		public int getBet() {
			return bet;
		}

		private boolean isAlreadyPlaye(IUser user) {
			return numsPlayers.containsValue(user);
		}

		private boolean isAlreadyBet(int num) {
			return numsPlayers.containsKey(num);
		}

		private void start() {
			EmbedBuilder builder = new EmbedBuilder()
					.withAuthorName("Jeu de dés")
					.withAuthorIcon(channel.getClient().getOurUser().getAvatarURL())
					.withThumbnail("http://findicons.com/files/icons/2118/nuvola/128/package_games_board.png")
					.withColor(new Color(170, 196, 222))
					.appendField(croupier.getName() + " a démarré un jeu de dés.",
							"Utilisez la commande `/dice <num>` pour rejoindre la partie avec une mise de **" + bet + " coins**.", false)
					.withFooterText("Vous avez " + (timer.getDelay()/1000) + " secondes pour faire vos paris");
			BotUtils.sendEmbed(builder.build(), channel);

			this.timer.start();
		}

		private void stop() {
			this.timer.stop();

			int rand = Utils.rand(1, 6);
			BotUtils.sendMessage(Emoji.DICE + " Le dés est lancé... **" + rand + "** !", channel);

			if(numsPlayers.containsKey(rand)) {
				IUser winner = numsPlayers.get(rand);
				int gains = bet*numsPlayers.size()*MULTIPLIER;
				BotUtils.sendMessage(Emoji.DICE + " Bravo " + winner.mention() + ", tu remportes " + gains + " coins !", channel);
				Storage.getUser(channel.getGuild(), winner).addCoins(gains);
				numsPlayers.remove(rand);
			}

			if(numsPlayers.size() > 0) {
				StringBuilder strBuilder = new StringBuilder(Emoji.LOST_MONEY + " Désolé, ");
				for(int num : numsPlayers.keySet()) {
					if(rand != num) {
						Storage.getUser(channel.getGuild(), numsPlayers.get(num)).addCoins(-bet);
						strBuilder.append(numsPlayers.get(num).mention() + ", ");
					}
				}
				strBuilder.append("vous avez perdu " + bet + " coin(s).");
				BotUtils.sendMessage(strBuilder.toString(), channel);
			}

			GUILDS_DICE.remove(channel.getGuild());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Aide pour la commande /" + context.getArg())
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(new Color(170, 196, 222))
				.appendDescription("**Lance une partie de dés avec une mise commune ou rejoint une partie déjà existante.**")
				.appendField("Utilisation", "Créer une partie : **/dice <mise> <num>**.\nRejoindre une partie **/dice <num>**", false)
				.appendField("Restrictions", "Le numéro doit être compris entre 1 et 6.\nVous ne pouvez pas miser un numéro qui a déjà été choisi par un autre utilisateur.", false)
				.appendField("Gains", "Le gagnant remporte " + MULTIPLIER + " fois la mise commune multipliée par le nombre de participants.", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
