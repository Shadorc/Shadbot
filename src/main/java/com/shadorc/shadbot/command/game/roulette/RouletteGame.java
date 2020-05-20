package com.shadorc.shadbot.command.game.roulette;

import com.shadorc.shadbot.command.game.roulette.RouletteCmd.Place;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.MultiplayerGame;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.*;
import io.prometheus.client.Summary;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RouletteGame extends MultiplayerGame<RouletteCmd, RoulettePlayer> {

    private static final Summary ROULETTE_SUMMARY = Summary.build().name("game_roulette")
            .help("Roulette game").labelNames("result").register();

    private static final List<Integer> RED_NUMS = List.of(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36);
    private static final Map<Place, Predicate<Integer>> TESTS = Map.of(
            Place.RED, RED_NUMS::contains,
            Place.BLACK, Predicate.not(RED_NUMS::contains),
            Place.LOW, num -> NumberUtils.isBetween(num, 1, 19),
            Place.HIGH, num -> NumberUtils.isBetween(num, 19, 37),
            Place.EVEN, num -> num % 2 == 0,
            Place.ODD, num -> num % 2 != 0);

    private final UpdatableMessage updatableMessage;

    private long startTime;
    private String results;

    public RouletteGame(RouletteCmd gameCmd, Context context) {
        super(gameCmd, context, Duration.ofSeconds(30));
        this.updatableMessage = new UpdatableMessage(context.getClient(), context.getChannelId());
    }

    @Override
    public Mono<Void> start() {
        return Mono.fromRunnable(() -> {
            this.schedule(this.end());
            this.startTime = System.currentTimeMillis();
            RouletteInputs.create(this.getContext().getClient(), this).listen();
        });
    }

    @Override
    public Mono<Void> end() {
        final int winningPlace = ThreadLocalRandom.current().nextInt(1, 37);
        return Flux.fromIterable(this.getPlayers().values())
                .flatMap(player -> Mono.zip(Mono.just(player), player.getUsername(this.getContext().getClient())))
                .flatMap(tuple -> {
                    final RoulettePlayer player = tuple.getT1();
                    final String username = tuple.getT2();
                    final Place place = Utils.parseEnum(Place.class, player.getPlace());

                    final int multiplier = RouletteGame.getMultiplier(player, place, winningPlace);
                    if (multiplier > 0) {
                        final long gains = Math.min(player.getBet() * multiplier, Config.MAX_COINS);
                        ROULETTE_SUMMARY.labels("win").observe(gains);
                        return player.win(gains)
                                .thenReturn(String.format("**%s** (Gains: **%s**)", username, FormatUtils.coins(gains)));
                    } else {
                        ROULETTE_SUMMARY.labels("loss").observe(player.getBet());
                        return Mono.just(String.format("**%s** (Losses: **%s**)", username, FormatUtils.coins(player.getBet())));
                    }
                })
                .collectSortedList()
                .doOnNext(list -> this.results = String.join(", ", list))
                .then(this.getContext().getChannel())
                .flatMap(channel -> DiscordUtils.sendMessage(
                        String.format(Emoji.DICE + " No more bets. *The wheel is spinning...* **%d (%s)** !",
                                winningPlace, RED_NUMS.contains(winningPlace) ? "Red" : "Black"), channel))
                .then(this.show())
                .then(Mono.fromRunnable(this::stop));
    }

    private static int getMultiplier(RoulettePlayer player, Place place, int winningPlace) {
        if (player.getPlace().equals(Integer.toString(winningPlace))) {
            return 36;
        } else if (place != null && TESTS.get(place).test(winningPlace)) {
            return 2;
        } else {
            return 0;
        }
    }

    @Override
    public Mono<Void> show() {
        return Flux.fromIterable(this.getPlayers().values())
                .flatMap(player -> Mono.zip(Mono.just(player), player.getUsername(this.getContext().getClient())))
                .collectList()
                .map(list -> DiscordUtils.getDefaultEmbed()
                        .andThen(embed -> {
                            final String description = String.format("**Use `%s%s <bet> <place>` to join the game.**"
                                            + "%n%n**place** is a `number between 1 and 36`, %s",
                                    this.getContext().getPrefix(), this.getContext().getCommandName(),
                                    FormatUtils.format(Place.values(),
                                            value -> String.format("`%s`", value.toString().toLowerCase()), ", "));
                            final String player = FormatUtils.format(list,
                                    tuple -> String.format("**%s** (%s)",
                                            tuple.getT2(), FormatUtils.coins(tuple.getT1().getBet())), "\n");
                            final String place = this.getPlayers().values().stream()
                                    .map(RoulettePlayer::getPlace)
                                    .collect(Collectors.joining("\n"));

                            embed.setAuthor("Roulette Game", null, this.getContext().getAvatarUrl())
                                    .setThumbnail("https://i.imgur.com/D7xZd6C.png")
                                    .setDescription(description)
                                    .addField("Player (Bet)", player, true)
                                    .addField("Place", place, true);

                            if (this.results != null) {
                                embed.addField("Results", this.results, false);
                            }

                            if (this.isScheduled()) {
                                final Duration remainingDuration = this.getDuration()
                                        .minusMillis(TimeUtils.getMillisUntil(this.startTime));
                                embed.setFooter(String.format("You have %d seconds to make your bets. Use %scancel to force the stop.",
                                        remainingDuration.toSeconds(), this.getContext().getPrefix()), null);
                            } else {
                                embed.setFooter("Finished.", null);
                            }
                        }))
                .map(this.updatableMessage::setEmbed)
                .flatMap(UpdatableMessage::send)
                .then();
    }

}