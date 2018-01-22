package me.shadorc.shadbot.command.game.roulette;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.game.AbstractGameManager;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.data.stats.Stats.MoneyEnum;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.Pair;
import me.shadorc.shadbot.utils.object.UpdateableMessage;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class RouletteManager extends AbstractGameManager {

	protected static final List<Integer> RED_NUMS = Arrays.asList(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36);

	private static final int GAME_DURATION = 30;

	// User, Pair<Bet, Place>
	private final ConcurrentHashMap<IUser, Pair<Integer, String>> playersPlace;
	private final UpdateableMessage message;

	private String results;

	public RouletteManager(AbstractCommand cmd, String prefix, IChannel channel, IUser author) {
		super(cmd, prefix, channel, author);
		this.playersPlace = new ConcurrentHashMap<>();
		this.message = new UpdateableMessage(channel);
	}

	@Override
	public void start() {
		this.schedule(() -> this.stop(), GAME_DURATION, TimeUnit.SECONDS);
	}

	public void show() {
		EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
				.setLenient(true)
				.withAuthorName("Roulette Game")
				.withThumbnail("http://icongal.com/gallery/image/278586/roulette_baccarat_casino.png")
				.withDescription(String.format("**Use `%s%s <bet> <place>` to join the game.**"
						+ "%n%n**Place** is a `number between 1 and 36`, `red`, `black`, `even`, `odd`, `low` or `high`",
						this.getPrefix(), this.getCmdName()))
				.appendField("Player (Bet)", FormatUtils.format(playersPlace.keySet().stream(),
						user -> String.format("**%s** (%s)", user.getName(), FormatUtils.formatCoins(playersPlace.get(user).getFirst())), "\n"), true)
				.appendField("Place", playersPlace.values().stream().map(Pair::getSecond).collect(Collectors.joining("\n")), true)
				.appendField("Results", results, false)
				.withFooterText(String.format("You have %d seconds to make your bets.", GAME_DURATION));
		message.send(embed.build()).get();
	}

	@Override
	public void stop() {
		this.cancelScheduledTask();

		int winningPlace = ThreadLocalRandom.current().nextInt(1, 37);

		List<String> list = new ArrayList<>();
		for(IUser user : playersPlace.keySet()) {
			int gains = playersPlace.get(user).getFirst();
			String place = playersPlace.get(user).getSecond();

			Map<String, Boolean> testsMap = new HashMap<>();
			testsMap.put("red", RED_NUMS.contains(winningPlace));
			testsMap.put("black", !RED_NUMS.contains(winningPlace));
			testsMap.put("low", Utils.isInRange(winningPlace, 1, 19));
			testsMap.put("high", Utils.isInRange(winningPlace, 19, 37));
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
				list.add(0, String.format("**%s** (Gains: **%s**)", user.getName(), FormatUtils.formatCoins(gains)));
				StatsManager.increment(MoneyEnum.MONEY_GAINED, this.getCmdName(), gains);
			} else {
				list.add(String.format("**%s** (Losses: **%s**)", user.getName(), FormatUtils.formatCoins(Math.abs(gains))));
				StatsManager.increment(MoneyEnum.MONEY_LOST, this.getCmdName(), Math.abs(gains));
			}
			Database.getDBUser(this.getGuild(), user).addCoins(gains);
		}

		BotUtils.sendMessage(String.format(Emoji.DICE + " No more bets. *The wheel is spinning...* **%d (%s)** !",
				winningPlace, RED_NUMS.contains(winningPlace) ? "Red" : "Black"),
				this.getChannel()).get();

		this.results = FormatUtils.format(list, Object::toString, ", ");
		this.show();

		playersPlace.clear();
		RouletteCmd.MANAGERS.remove(this.getChannel().getLongID());
	}

	protected boolean addPlayer(IUser user, Integer bet, String place) {
		if(playersPlace.putIfAbsent(user, new Pair<Integer, String>(bet, place)) == null) {
			this.show();
			return true;
		}
		return false;
	}

}