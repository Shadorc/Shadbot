package me.shadorc.discordbot.command.game.dice;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.swing.Timer;

import me.shadorc.discordbot.command.CommandManager;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.data.LottoDataManager;
import me.shadorc.discordbot.data.StatCategory;
import me.shadorc.discordbot.data.StatsManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

class DiceManager {

	protected static final ConcurrentHashMap<Long, DiceManager> CHANNELS_DICE = new ConcurrentHashMap<>();

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
				.appendField(context.getAuthorName() + " started a dice game.",
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
			boolean hasWon = num == winningNum;
			if(hasWon) {
				gains *= numsPlayers.size() + DiceCmd.MULTIPLIER;
				winningList.add("**" + user.getName() + "**, you win **" + StringUtils.pluralOf(gains, "coin") + "**");
			} else {
				loserList.add("**" + user.getName() + "** (Losses: **" + StringUtils.pluralOf(gains, "coin") + ")**");
				LottoDataManager.addToPool(gains);
			}
			DatabaseManager.addCoins(context.getGuild(), user, (hasWon ? 1 : -1) * gains);
			StatsManager.increment(hasWon ? StatCategory.MONEY_GAINS_COMMAND : StatCategory.MONEY_LOSSES_COMMAND,
					CommandManager.getCommand(context.getCommand()).getFirstName(), Math.abs(gains));
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
