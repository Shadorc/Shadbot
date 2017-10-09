package me.shadorc.discordbot.command.game;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.swing.Timer;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.Stats;
import me.shadorc.discordbot.data.StatCategory;
import me.shadorc.discordbot.data.Storage;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class DiceCmd extends AbstractCommand {

	protected static final ConcurrentHashMap<IChannel, DiceManager> CHANNELS_DICE = new ConcurrentHashMap<>();
	protected static final int MULTIPLIER = 6;

	private final RateLimiter rateLimiter;

	public DiceCmd() {
		super(CommandCategory.GAME, Role.USER, "dice");
		this.rateLimiter = new RateLimiter(RateLimiter.GAME_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		if(CHANNELS_DICE.containsKey(context.getChannel())) {
			this.joinGame(context);
		} else {
			this.createGame(context);
		}
	}

	private void createGame(Context context) throws MissingArgumentException {
		String[] splitArgs = StringUtils.getSplittedArg(context.getArg());
		if(splitArgs.length != 2) {
			throw new MissingArgumentException();
		}

		String betStr = splitArgs[0];
		if(!StringUtils.isPositiveInt(betStr)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid bet.", context.getChannel());
			return;
		}

		int bet = Integer.parseInt(betStr);
		if(context.getUser().getCoins() < bet) {
			BotUtils.sendMessage(Emoji.BANK + " You don't have enough coins for this.", context.getChannel());
			return;
		}

		String numStr = splitArgs[1];
		if(!StringUtils.isValidDiceNum(numStr)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid number, must be between 1 and 6.", context.getChannel());
			return;
		}

		int num = Integer.parseInt(numStr);

		DiceManager diceManager = new DiceManager(context, bet);
		diceManager.addPlayer(context.getAuthor(), num);
		diceManager.start();
		CHANNELS_DICE.putIfAbsent(context.getChannel(), diceManager);
	}

	private void joinGame(Context context) {
		DiceManager diceManager = CHANNELS_DICE.get(context.getChannel());
		if(context.getUser().getCoins() < diceManager.getBet()) {
			BotUtils.sendMessage(Emoji.BANK + " You don't have enough coins to join this game.", context.getChannel());
			return;
		}

		if(diceManager.isPlaying(context.getAuthor())) {
			BotUtils.sendMessage(Emoji.INFO + " You're already participating.", context.getChannel());
			return;
		}

		if(diceManager.getPlayers() == 6) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Sorry, there are already 6 players.", context.getChannel());
			return;
		}

		String numStr = context.getArg();
		if(!StringUtils.isValidDiceNum(numStr)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid number, must be between 1 and 6.", context.getChannel());
			return;
		}

		int num = Integer.parseInt(numStr);

		if(diceManager.isBet(num)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " This number has already been bet, please try with another one.", context.getChannel());
			return;
		}

		diceManager.addPlayer(context.getAuthor(), num);
		BotUtils.sendMessage(Emoji.DICE + " **" + context.getAuthorName() + "** bets on **" + num + "**.", context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Start a dice game with a common bet or join a game in progress.**")
				.appendField("Usage", "**Create a game:** `" + context.getPrefix() + "dice <bet> <num>`"
						+ "\n**Join a game:** `" + context.getPrefix() + "dice <num>`", false)
				.appendField("Restrictions", "**num** - must be between 1 and 6"
						+ "\nYou can't bet on a number that has already been chosen by another player.", false)
				.appendField("Gains", "The winner gets the common bet multiplied by " + MULTIPLIER + " plus the number of players."
						+ "\ngains = bet * (" + MULTIPLIER + " + players)", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

	protected class DiceManager {

		private static final int GAME_DURATION = 30;

		private final ConcurrentHashMap<Integer, IUser> numsPlayers;
		private final Context context;
		private final Timer timer;
		private final int bet;

		protected DiceManager(Context context, int bet) {
			this.context = context;
			this.bet = bet;
			this.numsPlayers = new ConcurrentHashMap<>();
			this.timer = new Timer((int) TimeUnit.SECONDS.toMillis(GAME_DURATION), event -> {
				this.stop();
			});
		}

		protected void start() {
			EmbedBuilder builder = Utils.getDefaultEmbed()
					.withAuthorName("Dice Game")
					.withThumbnail("http://findicons.com/files/icons/2118/nuvola/128/package_games_board.png")
					.appendField(context.getAuthor().getName() + " started a dice game.",
							"Use `" + context.getPrefix() + "dice <num>` to join the game with a **" + bet + " coins** putting.", false)
					.withFooterText("You have " + TimeUnit.MILLISECONDS.toSeconds(timer.getDelay()) + " seconds to make your bets.");
			BotUtils.sendMessage(builder.build(), context.getChannel());

			timer.start();
		}

		protected void stop() {
			timer.stop();

			int winningNum = MathUtils.rand(1, 6);
			BotUtils.sendMessage(Emoji.DICE + " The dice is rolling... **" + winningNum + "** !", context.getChannel());

			if(this.isBet(winningNum)) {
				IUser winner = numsPlayers.get(winningNum);
				int gains = bet * (numsPlayers.size() + MULTIPLIER);
				BotUtils.sendMessage(Emoji.DICE + " Congratulations **" + winner.getName() + "**, you win **" + gains + " coins** !", context.getChannel());
				Storage.getUser(context.getGuild(), winner).addCoins(gains);
				Stats.increment(StatCategory.MONEY_GAINS_COMMAND, DiceCmd.this.getNames()[0], gains);
			}

			List<IUser> losersList = numsPlayers.keySet().stream()
					.filter(num -> num != winningNum) // Remove winning number
					.map(num -> numsPlayers.get(num)) // Get losers
					.collect(Collectors.toList());

			if(!losersList.isEmpty()) {
				StringBuilder strBuilder = new StringBuilder(Emoji.MONEY_WINGS + " Sorry, ");
				for(IUser loser : losersList) {
					Storage.getUser(context.getGuild(), loser).addCoins(-bet);
					Stats.increment(StatCategory.MONEY_LOSSES_COMMAND, DiceCmd.this.getNames()[0], bet);
					strBuilder.append("**" + loser.getName() + "**, ");
				}
				strBuilder.append("you lost **" + StringUtils.pluralOf(bet, "coin") + "**.");
				BotUtils.sendMessage(strBuilder.toString(), context.getChannel());
			}

			CHANNELS_DICE.remove(context.getChannel());
		}

		public int getBet() {
			return bet;
		}

		public int getPlayers() {
			return numsPlayers.size();
		}

		protected void addPlayer(IUser user, int num) {
			numsPlayers.putIfAbsent(num, user);
		}

		protected boolean isPlaying(IUser user) {
			return numsPlayers.containsValue(user);
		}

		protected boolean isBet(int num) {
			return numsPlayers.containsKey(num);
		}
	}
}
