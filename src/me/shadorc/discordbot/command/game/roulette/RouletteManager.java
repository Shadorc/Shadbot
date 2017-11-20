package me.shadorc.discordbot.command.game.roulette;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import me.shadorc.discordbot.command.CommandManager;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.data.StatsManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.FormatUtils;
import me.shadorc.discordbot.utils.MathUtils;
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
	private final ScheduledExecutorService executor;

	protected RouletteManager(Context context) {
		this.context = context;
		this.playersPlace = new ConcurrentHashMap<>();
		this.executor = Executors.newSingleThreadScheduledExecutor();
	}

	protected void start() {
		EmbedBuilder builder = Utils.getDefaultEmbed()
				.withAuthorName("Roulette Game")
				.withThumbnail("http://icongal.com/gallery/image/278586/roulette_baccarat_casino.png")
				.appendField(context.getAuthorName() + " started a Roulette game.",
						"Use `" + context.getPrefix() + "roulette <bet> <place>` to join the game."
								+ "\n\n**place** - must be a number between 1 and 36, red, black, even, odd, low or high", false)
				.withFooterText("You have " + GAME_DURATION + " seconds to make your bets.");
		BotUtils.sendMessage(builder.build(), context.getChannel()).get();

		executor.schedule(() -> this.stop(), GAME_DURATION, TimeUnit.SECONDS);
	}

	protected void stop() {
		executor.shutdownNow();

		int winningPlace = MathUtils.rand(1, 36);

		List<String> list = new ArrayList<>();
		for(IUser user : playersPlace.keySet()) {
			int gains = playersPlace.get(user).getLeft();
			String place = playersPlace.get(user).getRight();

			Map<String, Boolean> testsMap = new HashMap<>();
			testsMap.put("red", RED_NUMS.contains(winningPlace));
			testsMap.put("black", !RED_NUMS.contains(winningPlace));
			testsMap.put("low", MathUtils.inRange(winningPlace, 1, 19));
			testsMap.put("high", MathUtils.inRange(winningPlace, 19, 37));
			testsMap.put("even", winningPlace % 2 == 0);
			testsMap.put("odd", winningPlace % 2 != 0);

			int multiplier = 0;
			if(place.equals(Integer.toString(winningPlace))) {
				multiplier = 36;
			} else if(testsMap.get(place)) {
				multiplier = 2;
			} else {
				multiplier = -1;
			}

			gains *= multiplier;
			if(gains > 0) {
				list.add(0, "**" + user.getName() + "** (Gains: **" + FormatUtils.formatCoins(gains) + ")**");
			} else {
				list.add("**" + user.getName() + "** (Losses: **" + FormatUtils.formatCoins(gains) + ")**");
			}
			DatabaseManager.addCoins(context.getChannel(), user, gains);
			StatsManager.updateGameStats(CommandManager.getFirstName(context.getCommand()), gains);
		}

		BotUtils.sendMessage(Emoji.DICE + " No more bets. *The wheel is spinning...* **" + winningPlace
				+ " (" + (RED_NUMS.contains(winningPlace) ? "Red" : "Black") + ")** !"
				+ "\n" + Emoji.BANK + " __Results:__ " + FormatUtils.formatList(list, str -> str.toString(), ", ") + ".", context.getChannel());

		playersPlace.clear();
		CHANNELS_ROULETTE.remove(context.getChannel().getLongID());
	}

	protected void addPlayer(IUser user, Integer bet, String place) {
		playersPlace.putIfAbsent(user, new Pair<Integer, String>(bet, place));
	}

	protected boolean isPlaying(IUser user) {
		return playersPlace.containsKey(user);
	}
}