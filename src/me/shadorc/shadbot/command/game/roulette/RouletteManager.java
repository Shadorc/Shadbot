package me.shadorc.shadbot.command.game.roulette;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.command.game.roulette.RouletteCmd.Place;
import me.shadorc.shadbot.core.command.CommandInitializer;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.game.GameCmd;
import me.shadorc.shadbot.core.game.GameManager;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.MoneyEnum;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.object.message.UpdateableMessage;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class RouletteManager extends GameManager {

	private static final List<Integer> RED_NUMS = List.of(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36);
	private static final Map<Place, Predicate<Integer>> TESTS = Map.of(
			Place.RED, RED_NUMS::contains,
			Place.BLACK, Predicate.not(RED_NUMS::contains),
			Place.LOW, num -> NumberUtils.isInRange(num, 1, 19),
			Place.HIGH, num -> NumberUtils.isInRange(num, 19, 37),
			Place.EVEN, num -> num % 2 == 0,
			Place.ODD, num -> num % 2 != 0);

	private final Map<Snowflake, RoulettePlayer> players;
	private final UpdateableMessage updateableMessage;

	private long startTime;
	private String results;

	public RouletteManager(GameCmd<RouletteManager> gameCmd, Context context) {
		super(gameCmd, context, Duration.ofSeconds(30));
		this.players = new ConcurrentHashMap<>();
		this.updateableMessage = new UpdateableMessage(context.getClient(), context.getChannelId());
	}

	@Override
	public void start() {
		this.schedule(this.end());
		this.startTime = System.currentTimeMillis();
		new RouletteInputs(this.getContext().getClient(), this).subscribe();
	}

	@Override
	public Mono<Void> show() {
		return Flux.fromIterable(this.players.values())
				.flatMap(player -> Mono.zip(Mono.just(player),
						player.getUsername(this.getContext().getClient())))
				.collectList()
				.map(list -> EmbedUtils.getDefaultEmbed()
						.andThen(embed -> {
							embed.setAuthor("Roulette Game", null, this.getContext().getAvatarUrl())
									.setThumbnail("http://icongal.com/gallery/image/278586/roulette_baccarat_casino.png")
									.setDescription(String.format("**Use `%s%s <bet> <place>` to join the game.**"
											+ "%n%n**place** is a `number between 1 and 36`, %s",
											this.getContext().getPrefix(), this.getContext().getCommandName(),
											FormatUtils.format(Place.values(), value -> String.format("`%s`", StringUtils.toLowerCase(value)), ", ")))
									.addField("Player (Bet)", FormatUtils.format(list,
											tuple -> String.format("**%s** (%s)", tuple.getT2(), FormatUtils.coins(tuple.getT1().getBet())), "\n"), true)
									.addField("Place", String.join("\n", this.players.values().stream().map(RoulettePlayer::getPlace).collect(Collectors.toList())), true);

							if(this.results != null) {
								embed.addField("Results", this.results, false);
							}

							if(this.isScheduled()) {
								final Duration remainingDuration = this.getDuration().minusMillis(TimeUtils.getMillisUntil(this.startTime));
								embed.setFooter(String.format("You have %d seconds to make your bets. Use %scancel to force the stop.",
										remainingDuration.toSeconds(), this.getContext().getPrefix()), null);
							} else {
								embed.setFooter("Finished.", null);
							}
						}))
				.flatMap(this.updateableMessage::send)
				.then();
	}

	private Mono<Void> end() {
		final int winningPlace = ThreadLocalRandom.current().nextInt(1, 37);
		return Flux.fromIterable(this.players.values())
				.flatMap(player -> Mono.zip(Mono.just(player),
						player.getUsername(this.getContext().getClient())))
				.map(tuple -> {
					final RoulettePlayer player = tuple.getT1();
					final String username = tuple.getT2();
					final Place placeEnum = Utils.parseEnum(Place.class, player.getPlace());

					int multiplier = 0;
					if(player.getPlace().equals(Integer.toString(winningPlace))) {
						multiplier = 36;
					} else if(placeEnum != null && TESTS.get(placeEnum).test(winningPlace)) {
						multiplier = 2;
					} else {
						multiplier = -1;
					}

					final int gains = player.getBet() * multiplier;
					Shadbot.getDatabase().getDBMember(this.getContext().getGuildId(), player.getUserId()).addCoins(gains);

					if(gains > 0) {
						StatsManager.MONEY_STATS.log(MoneyEnum.MONEY_GAINED, CommandInitializer.getCommand(this.getContext().getCommandName()).getName(), gains);
						return String.format("**%s** (Gains: **%s**)", username, FormatUtils.coins(gains));
					} else {
						StatsManager.MONEY_STATS.log(MoneyEnum.MONEY_LOST, CommandInitializer.getCommand(this.getContext().getCommandName()).getName(), Math.abs(gains));
						Shadbot.getLottery().addToJackpot(Math.abs(gains));
						return String.format("**%s** (Losses: **%s**)", username, FormatUtils.coins(Math.abs(gains)));
					}
				})
				.collectSortedList()
				.map(list -> this.results = String.join(", ", list))
				.then(this.getContext().getChannel())
				.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.DICE + " No more bets. *The wheel is spinning...* **%d (%s)** !",
						winningPlace, RED_NUMS.contains(winningPlace) ? "Red" : "Black"), channel))
				.then(this.show())
				.then(Mono.fromRunnable(this::stop));
	}

	protected boolean addPlayerIfAbsent(Snowflake userId, Integer bet, String place) {
		return this.players.putIfAbsent(userId, new RoulettePlayer(userId, bet, place)) == null;
	}

	protected Map<Snowflake, RoulettePlayer> getPlayers() {
		return Collections.unmodifiableMap(this.players);
	}

}