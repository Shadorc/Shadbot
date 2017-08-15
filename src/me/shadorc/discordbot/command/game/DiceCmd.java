package me.shadorc.discordbot.command.game;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Timer;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.Setting;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.StringUtils;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class DiceCmd extends AbstractCommand {

	protected static final Map<IGuild, DiceManager> GUILDS_DICE = new HashMap<>();
	protected static final int MULTIPLIER = 6;

	public DiceCmd() {
		super(false, "dice", "des");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(GUILDS_DICE.containsKey(context.getGuild())) {
			DiceManager diceManager = GUILDS_DICE.get(context.getGuild());

			if(context.getUser().getCoins() < diceManager.getBet()) {
				BotUtils.sendMessage(Emoji.BANK + " You don't have enough coins to join this game.", context.getChannel());
				return;
			}

			if(diceManager.isAlreadyPlaying(context.getAuthor())) {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " " + context.getAuthor().mention() + ", you're already participating.", context.getChannel());
				return;
			}

			if(context.getArg() == null || !StringUtils.isInteger(context.getArg())) {
				throw new MissingArgumentException();
			}

			int num = Integer.parseInt(context.getArg());

			if(num < 1 || num > 6) {
				throw new MissingArgumentException();
			}

			if(diceManager.hasAlreadyBet(num)) {
				BotUtils.sendMessage(Emoji.EXCLAMATION + " This number has already been bet, please try with another one.", context.getChannel());
				return;
			}

			diceManager.addPlayer(context.getAuthor(), num);
			BotUtils.sendMessage(Emoji.DICE + " " + context.getAuthor().mention() + " bet on " + num + ".", context.getChannel());

		} else {
			if(!context.hasArg()) {
				throw new MissingArgumentException();
			}

			String[] splitArgs = context.getArg().split(" ");
			if(splitArgs.length != 2 || !StringUtils.isInteger(splitArgs[0]) || !StringUtils.isInteger(splitArgs[1])) {
				throw new MissingArgumentException();
			}

			int num = Integer.parseInt(splitArgs[1]);
			if(num < 1 || num > 6) {
				throw new MissingArgumentException();
			}

			int bet = Integer.parseInt(splitArgs[0]);
			if(context.getUser().getCoins() < bet) {
				BotUtils.sendMessage(Emoji.BANK + " You don't have enough coins for this.", context.getChannel());
				return;
			}

			DiceManager diceManager = new DiceManager(context.getChannel(), context.getAuthor(), num, bet);
			GUILDS_DICE.put(context.getGuild(), diceManager);
			diceManager.start();
		}

	}

	protected class DiceManager {

		private final Map<Integer, IUser> numsPlayers;
		private final IChannel channel;
		private final IUser croupier;
		private final Timer timer;
		private final int bet;

		protected DiceManager(IChannel channel, IUser user, int num, int bet) {
			this.channel = channel;
			this.croupier = user;
			this.numsPlayers = new HashMap<>();
			this.numsPlayers.put(num, user);
			this.bet = bet;
			this.timer = new Timer(30 * 1000, event -> {
				this.stop();
			});
		}

		protected void addPlayer(IUser user, int num) {
			this.numsPlayers.put(num, user);
		}

		public int getBet() {
			return bet;
		}

		protected boolean isAlreadyPlaying(IUser user) {
			return numsPlayers.containsValue(user);
		}

		protected boolean hasAlreadyBet(int num) {
			return numsPlayers.containsKey(num);
		}

		protected void start() {
			EmbedBuilder builder = new EmbedBuilder()
					.withAuthorName("Dice Game")
					.withAuthorIcon(channel.getClient().getOurUser().getAvatarURL())
					.withThumbnail("http://findicons.com/files/icons/2118/nuvola/128/package_games_board.png")
					.withColor(Config.BOT_COLOR)
					.appendField(croupier.getName() + " started a dice game.",
							"Use `" + Storage.getSetting(channel.getGuild(), Setting.PREFIX) + "dice <num>` to join the game with a **" + bet + " coins** putting.", false)
					.withFooterText("You have " + (timer.getDelay() / 1000) + " seconds to make your bets.");
			BotUtils.sendEmbed(builder.build(), channel);

			this.timer.start();
		}

		protected void stop() {
			this.timer.stop();

			int rand = MathUtils.rand(1, 6);
			BotUtils.sendMessage(Emoji.DICE + " The dice is rolling... **" + rand + "** !", channel);

			if(numsPlayers.containsKey(rand)) {
				IUser winner = numsPlayers.get(rand);
				int gains = bet * numsPlayers.size() * MULTIPLIER;
				BotUtils.sendMessage(Emoji.DICE + " Congratulations " + winner.mention() + ", you win " + gains + " coins !", channel);
				Storage.getUser(channel.getGuild(), winner).addCoins(gains);
				numsPlayers.remove(rand);
			}

			if(!numsPlayers.isEmpty()) {
				StringBuilder strBuilder = new StringBuilder(Emoji.MONEY_WINGS + " Sorry, ");
				for(int num : numsPlayers.keySet()) {
					if(rand != num) {
						Storage.getUser(channel.getGuild(), numsPlayers.get(num)).addCoins(-bet);
						strBuilder.append(numsPlayers.get(num).mention() + ", ");
					}
				}
				strBuilder.append("you have lost " + bet + " coin(s).");
				BotUtils.sendMessage(strBuilder.toString(), channel);
			}

			GUILDS_DICE.remove(channel.getGuild());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Start a dice game with a common bet or join a game in progress.**")
				.appendField("Usage", "Create a game: **" + context.getPrefix() + "dice <bet> <num>**.\nJoin a game **" + context.getPrefix() + "dice <num>**", false)
				.appendField("Restrictions", "The number must be between 1 and 6.\nYou can't bet on a number that has already been chosen by another player.", false)
				.appendField("Gains", "The winner gets " + MULTIPLIER + " times the common bet multiplied by the number of players.", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
