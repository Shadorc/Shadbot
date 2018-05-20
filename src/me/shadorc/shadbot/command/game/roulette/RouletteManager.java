package me.shadorc.shadbot.command.game.roulette;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import me.shadorc.shadbot.command.game.roulette.RouletteCmd.Place;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.game.AbstractGameManager;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.data.stats.MoneyStatsManager;
import me.shadorc.shadbot.data.stats.MoneyStatsManager.MoneyEnum;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.UpdateableMessage;

public class RouletteManager extends AbstractGameManager {

	protected static final List<Integer> RED_NUMS = List.of(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36);

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
						+ "%n%n**Place** is a `number between 1 and 36`, %s",
						this.getPrefix(), this.getCmdName(),
						FormatUtils.format(Place.values(), value -> String.format("`%s`", value.toString().toLowerCase()), ", ")))
				.addField("Player (Bet)", FormatUtils.format(playersPlace.keySet().stream(),
						user -> String.format("**%s** (%s)", user.getName(), FormatUtils.formatCoins(playersPlace.get(user).getFirst())), "\n"), true)
				.addField("Place", playersPlace.values().stream().map(Pair::getSecond).collect(Collectors.joining("\n")), true)
				.addField("Results", results, false)
				.withFooterText(String.format("You have %d seconds to make your bets.", GAME_DURATION));

		RequestFuture<IMessage> msgRequest = message.send(embed.build());
		if(msgRequest != null) {
			msgRequest.get();
		}
	}

	@Override
	public void stop() {
		this.cancelScheduledTask();

		int winningPlace = ThreadLocalRandom.current().nextInt(1, 37);

		List<String> list = new ArrayList<>();
		for(IUser user : playersPlace.keySet()) {
			int gains = playersPlace.get(user).getFirst();
			String place = playersPlace.get(user).getSecond();
			Place placeEnum = Utils.getValueOrNull(Place.class, place);

			Map<Place, Boolean> testsMap = new HashMap<>();
			testsMap.put(Place.RED, RED_NUMS.contains(winningPlace));
			testsMap.put(Place.BLACK, !RED_NUMS.contains(winningPlace));
			testsMap.put(Place.LOW, Utils.isInRange(winningPlace, 1, 19));
			testsMap.put(Place.HIGH, Utils.isInRange(winningPlace, 19, 37));
			testsMap.put(Place.EVEN, winningPlace % 2 == 0);
			testsMap.put(Place.ODD, winningPlace % 2 != 0);

			int multiplier = 0;
			if(place.equals(Integer.toString(winningPlace))) {
				multiplier = 36;
			} else if(placeEnum != null && testsMap.get(placeEnum)) {
				multiplier = 2;
			} else {
				multiplier = -1;
			}

			testsMap.clear();

			gains *= multiplier;
			if(gains > 0) {
				list.add(0, String.format("**%s** (Gains: **%s**)", user.getName(), FormatUtils.formatCoins(gains)));
				MoneyStatsManager.log(MoneyEnum.MONEY_GAINED, this.getCmdName(), gains);
			} else {
				list.add(String.format("**%s** (Losses: **%s**)", user.getName(), FormatUtils.formatCoins(Math.abs(gains))));
				MoneyStatsManager.log(MoneyEnum.MONEY_LOST, this.getCmdName(), Math.abs(gains));
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