package me.shadorc.discordbot.command.game.roulette;

import java.util.ArrayList;
import java.util.Arrays;
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
import me.shadorc.discordbot.utils.game.Pair;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

class RouletteManager {

	protected static final ConcurrentHashMap<Long, RouletteManager> CHANNELS_ROULETTE = new ConcurrentHashMap<>();
	protected static final List<Integer> RED_NUMS = Arrays.asList(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36);

	private static final int GAME_DURATION = 30;

	private final ConcurrentHashMap<IUser, Pair<Integer, String>> playersPlace;
	private final Context context;
	private final Timer timer;

	protected RouletteManager(Context context) {
		this.context = context;
		this.playersPlace = new ConcurrentHashMap<>();
		this.timer = new Timer((int) TimeUnit.SECONDS.toMillis(GAME_DURATION), event -> {
			this.stop();
		});
	}

	protected void start() {
		EmbedBuilder builder = Utils.getDefaultEmbed()
				.withAuthorName("Roulette Game")
				.withThumbnail("http://icongal.com/gallery/image/278586/roulette_baccarat_casino.png")
				.appendField(context.getAuthorName() + " started a Roulette game.",
						"Use `" + context.getPrefix() + "roulette <bet> <place>` to join the game."
								+ "\n\n**place** - must be a number between 1 and 36, red, black, even, odd, low or high", false)
				.withFooterText("You have " + TimeUnit.MILLISECONDS.toSeconds(timer.getDelay()) + " seconds to make your bets.");
		BotUtils.sendMessage(builder.build(), context.getChannel()).get();

		timer.start();
	}

	protected void stop() {
		timer.stop();

		int winningPlace = MathUtils.rand(1, 36);

		List<String> winningList = new ArrayList<>();
		List<String> loserList = new ArrayList<>();

		for(IUser user : playersPlace.keySet()) {
			int gains = playersPlace.get(user).getLeft();
			String place = playersPlace.get(user).getRight();

			if(StringUtils.isPositiveInt(place) && Integer.parseInt(place) == winningPlace) {
				gains *= 36;
				winningList.add("**" + user.getName() + "** (Gains: **" + StringUtils.pluralOf(gains, "coin") + "**)");
				DatabaseManager.addCoins(context.getGuild(), user, gains);
				StatsManager.increment(StatCategory.MONEY_GAINS_COMMAND, CommandManager.getFirstName(context.getCommand()), gains);

			} else if(StringUtils.isPositiveInt(place) && this.isRed(winningPlace) && this.isRed(Integer.parseInt(place))
					|| StringUtils.isPositiveInt(place) && !this.isRed(winningPlace) && !this.isRed(Integer.parseInt(place))
					|| MathUtils.inRange(winningPlace, 1, 19) && "low".equals(place)
					|| MathUtils.inRange(winningPlace, 19, 37) && "high".equals(place)
					|| winningPlace % 2 == 0 && "even".equals(place)
					|| winningPlace % 2 != 0 && "odd".equals(place)) {
				gains *= 2;
				winningList.add("**" + user.getName() + "** (Gains: **" + StringUtils.pluralOf(gains, "coin") + "**)");
				DatabaseManager.addCoins(context.getGuild(), user, gains);
				StatsManager.increment(StatCategory.MONEY_GAINS_COMMAND, CommandManager.getFirstName(context.getCommand()), gains);

			} else {
				loserList.add("**" + user.getName() + "** (Losses: **" + StringUtils.pluralOf(gains, "coin") + ")**");
				DatabaseManager.addCoins(context.getGuild(), user, -gains);
				StatsManager.increment(StatCategory.MONEY_LOSSES_COMMAND, CommandManager.getFirstName(context.getCommand()), gains);
				LottoDataManager.addToPool(gains);
			}
		}

		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append(Emoji.DICE + " No more bets. *The wheel is spinning...* **" + winningPlace
				+ " (" + (isRed(winningPlace) ? "Red" : "Black") + ")** !");

		if(!winningList.isEmpty()) {
			strBuilder.append("\n" + Emoji.MONEY_BAG + " Congratulations to " + StringUtils.formatList(winningList, str -> str, ", ") + ".");
		}
		if(!loserList.isEmpty()) {
			strBuilder.append("\n" + Emoji.MONEY_WINGS + " Sorry, " + StringUtils.formatList(loserList, str -> str, ", ") + ".");
		}
		BotUtils.sendMessage(strBuilder.toString(), context.getChannel());

		playersPlace.clear();
		CHANNELS_ROULETTE.remove(context.getChannel().getLongID());
	}

	protected void addPlayer(IUser user, Integer bet, String place) {
		playersPlace.putIfAbsent(user, new Pair<Integer, String>(bet, place));
	}

	protected boolean isPlaying(IUser user) {
		return playersPlace.containsKey(user);
	}

	private boolean isRed(Integer num) {
		return RED_NUMS.contains(num);
	}
}