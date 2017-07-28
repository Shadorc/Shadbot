package me.shadorc.discordbot.command.game;

import java.awt.Color;
import java.util.HashMap;

import javax.swing.Timer;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Utils;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class DiceCmd extends Command {

	private static final HashMap <IGuild, DiceManager> GUILDS_DICE = new HashMap<>();

	public DiceCmd() {
		super(false, "des", "dice");
	}

	@Override
	public void execute(Context context) {
		if(!GUILDS_DICE.containsKey(context.getGuild())) {
			if(context.getArg() == null) {
				BotUtils.sendMessage(Emoji.WARNING + " Vous devez indiquer une mise et un chiffre.", context.getChannel());
				return;
			}

			String[] splitArgs = context.getArg().split(" ");
			if(splitArgs.length != 2) {
				BotUtils.sendMessage(Emoji.WARNING + " Vous devez indiquer une mise et un chiffre.", context.getChannel());
				return;
			}

			if(!Utils.isInteger(splitArgs[0]) || !Utils.isInteger(splitArgs[1])) {
				BotUtils.sendMessage(Emoji.WARNING + " La mise ou le chiffre que vous avez indiquez ne sont pas des nombres.", context.getChannel());
				return;
			}

			int bet = Integer.parseInt(splitArgs[0]);
			int num = Integer.parseInt(splitArgs[1]);

			if(num < 1 || num > 6) {
				BotUtils.sendMessage(Emoji.WARNING + " Vous devez indiquer un chiffre compris entre 1 et 6.", context.getChannel());
				return;
			}

			if(Storage.getCoins(context.getGuild(), context.getAuthor()) < bet) {
				BotUtils.sendMessage(Emoji.BANK + " Vous n'avez pas assez de coins pour effectuer cette mise.", context.getChannel());
				return;
			}

			GUILDS_DICE.put(context.getGuild(), new DiceManager(context.getChannel(), context.getAuthor(), num, bet));
		}

		else {
			if(Storage.getCoins(context.getGuild(), context.getAuthor()) < GUILDS_DICE.get(context.getGuild()).getBet()) {
				BotUtils.sendMessage(Emoji.BANK + " Vous n'avez pas assez de coins pour rejoindre cette partie.", context.getChannel());
				return;
			}

			if(GUILDS_DICE.get(context.getGuild()).isAlreadyPlaye(context.getAuthor())) {
				BotUtils.sendMessage(Emoji.WARNING + " " + context.getAuthor().mention() + ", tu participez déjà.", context.getChannel());
				return;
			}

			if(context.getArg() == null || !Utils.isInteger(context.getArg())) {
				BotUtils.sendMessage(Emoji.WARNING + " Vous devez indiquer un chiffre.", context.getChannel());
				return;
			}

			int num = Integer.parseInt(context.getArg());

			if(num < 1 || num > 6) {
				BotUtils.sendMessage(Emoji.WARNING + " Vous devez indiquer un chiffre compris entre 1 et 6.", context.getChannel());
				return;
			}

			if(GUILDS_DICE.get(context.getGuild()).isAlreadyBet(num)) {
				BotUtils.sendMessage(Emoji.WARNING + " Ce numéro a déjà été parié, merci d'en indiquer un autre.", context.getChannel());
				return;
			}

			GUILDS_DICE.get(context.getGuild()).addPlayer(context.getAuthor(), num);
			BotUtils.sendMessage(Emoji.DICE + " " + context.getAuthor().mention() + " a misé sur le " + num + ".", context.getChannel());
		}
	}

	public class DiceManager {

		private IChannel channel;
		private IUser croupier;
		private int bet;
		private HashMap<Integer, IUser> numsPlayers;
		private Timer timer;

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

		public void addPlayer(IUser user, int num) {
			this.numsPlayers.put(num, user);
		}

		public int getBet() {
			return bet;
		}

		public boolean isAlreadyPlaye(IUser user) {
			return numsPlayers.containsValue(user);
		}

		public boolean isAlreadyBet(int num) {
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
				int gains = bet*numsPlayers.size()*6;
				BotUtils.sendMessage(Emoji.DICE + " Bravo " + winner.mention() + ", tu remportes " + gains + " coins !", channel);
				Utils.addCoins(channel.getGuild(), winner, gains);
				numsPlayers.remove(rand);
			}

			if(numsPlayers.size() > 0) {
				StringBuilder strBuilder = new StringBuilder(Emoji.LOST_MONEY + " Désolé, ");
				for(int num : numsPlayers.keySet()) {
					if(rand != num) {
						Utils.addCoins(channel.getGuild(), numsPlayers.get(num), -bet);
						strBuilder.append(numsPlayers.get(num).mention() + ", ");
					}
				}
				strBuilder.append("vous avez perdu " + bet + " coin(s).");
				BotUtils.sendMessage(strBuilder.toString(), channel);
			}

			GUILDS_DICE.remove(channel.getGuild());
		}
	}
}
