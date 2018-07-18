package me.shadorc.shadbot.command.game.roulette;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.command.game.roulette.RouletteCmd.Place;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.game.AbstractGameManager;
import me.shadorc.shadbot.data.db.DatabaseManager;
import me.shadorc.shadbot.data.stats.MoneyStatsManager;
import me.shadorc.shadbot.data.stats.MoneyStatsManager.MoneyEnum;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.message.UpdateableMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class RouletteManager extends AbstractGameManager {

	protected static final List<Integer> RED_NUMS = List.of(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36);

	private static final int GAME_DURATION = 30;

	// User ID, Tuple2<Bet, Place>
	private final ConcurrentHashMap<Snowflake, Tuple2<Integer, String>> playersPlace;
	private final UpdateableMessage updateableMessage;

	private String results;

	public RouletteManager(Context context) {
		super(context);
		this.playersPlace = new ConcurrentHashMap<>();
		this.updateableMessage = new UpdateableMessage(context.getClient(), context.getChannelId());
	}

	@Override
	public Mono<Void> start() {
		this.schedule(() -> this.stop().subscribe(), GAME_DURATION, TimeUnit.SECONDS);
		return Mono.empty();
	}

	public Mono<Void> show() {
		Mono<List<User>> usernamesMono = Flux.fromIterable(playersPlace.keySet())
				.flatMap(userId -> this.getContext().getClient().getUserById(userId))
				.collectList();

		return this.getContext().getAvatarUrl()
				.zipWith(usernamesMono)
				.map(avatarUrlAndUsers -> {
					final String avatarUrl = avatarUrlAndUsers.getT1();
					final List<User> users = avatarUrlAndUsers.getT2();

					EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed()
							.setAuthor("Roulette Game", null, avatarUrl)
							.setThumbnail("http://icongal.com/gallery/image/278586/roulette_baccarat_casino.png")
							.setDescription(String.format("**Use `%s%s <bet> <place>` to join the game.**"
									+ "%n%n**Place** is a `number between 1 and 36`, %s",
									this.getContext().getPrefix(), this.getContext().getCommandName(),
									FormatUtils.format(Place.values(), value -> String.format("`%s`", value.toString().toLowerCase()), ", ")))
							.addField("Player (Bet)", FormatUtils.format(users,
									user -> String.format("**%s** (%s)", user.getUsername(), FormatUtils.formatCoins(playersPlace.get(user.getId()).getT1())), "\n"), true)
							.addField("Place", playersPlace.values().stream().map(Tuple2::getT2).collect(Collectors.joining("\n")), true);

					if(results != null) {
						embed.addField("Results", results, false);
					}

					if(!this.isTaskDone()) {
						embed.setFooter(String.format("You have %d seconds to make your bets.", GAME_DURATION), null);
					}

					return embed;
				})
				.flatMap(embed -> updateableMessage.send(embed))
				.then();
	}

	@Override
	public Mono<Void> stop() {
		this.cancelScheduledTask();
		RouletteCmd.MANAGERS.remove(this.getContext().getChannelId());

		final int winningPlace = ThreadLocalRandom.current().nextInt(1, 37);

		return Flux.fromIterable(playersPlace.keySet())
				.flatMap(userId -> this.getContext().getClient().getUserById(userId))
				.map(user -> {
					final int bet = playersPlace.get(user.getId()).getT1();
					final String place = playersPlace.get(user.getId()).getT2();
					final Place placeEnum = Utils.getEnum(Place.class, place);

					Map<Place, Boolean> testsMap = Map.of(
							Place.RED, RED_NUMS.contains(winningPlace),
							Place.BLACK, !RED_NUMS.contains(winningPlace),
							Place.LOW, NumberUtils.isInRange(winningPlace, 1, 19),
							Place.HIGH, NumberUtils.isInRange(winningPlace, 19, 37),
							Place.EVEN, winningPlace % 2 == 0,
							Place.ODD, winningPlace % 2 != 0);

					int multiplier = 0;
					if(place.equals(Integer.toString(winningPlace))) {
						multiplier = 36;
					} else if(placeEnum != null && testsMap.get(placeEnum)) {
						multiplier = 2;
					} else {
						multiplier = -1;
					}

					final int gains = bet * multiplier;
					DatabaseManager.getDBMember(this.getContext().getGuildId(), user.getId()).addCoins(gains);

					if(gains > 0) {
						MoneyStatsManager.log(MoneyEnum.MONEY_GAINED, this.getContext().getCommandName(), gains);
						return String.format("**%s** (Gains: **%s**)", user.getUsername(), FormatUtils.formatCoins(gains));
					} else {
						MoneyStatsManager.log(MoneyEnum.MONEY_LOST, this.getContext().getCommandName(), Math.abs(gains));
						return String.format("**%s** (Losses: **%s**)", user.getUsername(), FormatUtils.formatCoins(Math.abs(gains)));
					}
				})
				.collectSortedList()
				.map(list -> FormatUtils.format(list, Object::toString, ", "))
				.map(results -> this.results = results)
				.then(BotUtils.sendMessage(String.format(Emoji.DICE + " No more bets. *The wheel is spinning...* **%d (%s)** !",
						winningPlace, RED_NUMS.contains(winningPlace) ? "Red" : "Black"),
						this.getContext().getChannel()))
				.then(this.show());
	}

	/**
	 * @return true if the user was not participating, false otherwise
	 */
	protected boolean addPlayer(Snowflake userId, Integer bet, String place) {
		return playersPlace.putIfAbsent(userId, Tuples.of(bet, place)) == null;
	}

}