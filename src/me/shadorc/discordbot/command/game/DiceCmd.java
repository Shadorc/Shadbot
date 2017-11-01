package me.shadorc.discordbot.command.game;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.swing.Timer;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.StatCategory;
import me.shadorc.discordbot.data.Stats;
import me.shadorc.discordbot.data.Storage;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.TextUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import me.shadorc.discordbot.utils.game.GameUtils;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class DiceCmd extends AbstractCommand {

	protected static final ConcurrentHashMap<Long, DiceManager> CHANNELS_DICE = new ConcurrentHashMap<>();
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

		if(CHANNELS_DICE.containsKey(context.getChannel().getLongID())) {
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

		Integer bet = GameUtils.parseBetOrWarn(splitArgs[0], context);
		if(bet == null) {
			return;
		}

		String numStr = splitArgs[1];
		if(!StringUtils.isValidDiceNum(numStr)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid number, must be between 1 and 6.", context.getChannel());
			return;
		}

		int num = Integer.parseInt(numStr);

		DiceManager diceManager = CHANNELS_DICE.getOrDefault(context.getChannel().getLongID(), new DiceManager(context, bet));

		DiceManager currentManager = CHANNELS_DICE.putIfAbsent(context.getChannel().getLongID(), diceManager);
		if(currentManager == null) {
			diceManager.start();
		} else {
			diceManager = currentManager;
		}

		diceManager.addPlayer(context.getAuthor(), num);
		BotUtils.sendMessage(Emoji.DICE + " **" + context.getAuthorName() + "** bets on **" + num + "**.", context.getChannel());
	}

	private void joinGame(Context context) {
		DiceManager diceManager = CHANNELS_DICE.get(context.getChannel().getLongID());
		if(Storage.getCoins(context.getGuild(), context.getAuthor()) < diceManager.getBet()) {
			BotUtils.sendMessage(TextUtils.notEnoughCoins(context.getAuthor()), context.getChannel());
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
						+ "\n**Join a game:** `" + context.getPrefix() + this.getFirstName() + " <num>`", false)
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
			BotUtils.sendMessage(builder.build(), context.getChannel()).get();

			timer.start();
		}

		protected void stop() {
			timer.stop();

			int winningNum = MathUtils.rand(1, 6);

			List<String> winningList = new ArrayList<>();
			List<String> loserList = new ArrayList<>();

			for(int num : numsPlayers.keySet()) {
				IUser user = numsPlayers.get(num);
				int gains = bet;
				if(num == winningNum) {
					gains *= numsPlayers.size() + MULTIPLIER;
					winningList.add("**" + user.getName() + "**, you win **" + StringUtils.pluralOf(gains, "coin") + "**");
					Storage.addCoins(context.getGuild(), user, gains);
					Stats.increment(StatCategory.MONEY_GAINS_COMMAND, DiceCmd.this.getFirstName(), gains);
				} else {
					loserList.add("**" + user.getName() + "** (Losses: **" + StringUtils.pluralOf(gains, "coin") + ")**");
					Storage.addCoins(context.getGuild(), user, -gains);
					Stats.increment(StatCategory.MONEY_LOSSES_COMMAND, DiceCmd.this.getFirstName(), gains);
				}
			}

			StringBuilder strBuilder = new StringBuilder();

			strBuilder.append(Emoji.DICE + " The dice is rolling... **" + winningNum + "** !");
			if(!winningList.isEmpty()) {
				strBuilder.append("\n" + Emoji.MONEY_BAG + " Congratulations " + StringUtils.formatList(winningList, str -> str, ", ") + " !");
			}
			if(!loserList.isEmpty()) {
				strBuilder.append("\n" + Emoji.MONEY_WINGS + " Sorry, " + StringUtils.formatList(loserList, str -> str, ", ") + ".");
			}
			BotUtils.sendMessage(strBuilder.toString(), context.getChannel());

			numsPlayers.clear();
			CHANNELS_DICE.remove(context.getChannel().getLongID());
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
