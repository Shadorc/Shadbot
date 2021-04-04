package com.shadorc.shadbot.command.game.roulette;

import com.shadorc.shadbot.command.game.roulette.RouletteCmd.Place;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.MultiplayerGame;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.*;
import discord4j.discordjson.json.MessageData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RouletteGame extends MultiplayerGame<RoulettePlayer> {

    private static final List<Integer> RED_NUMS =
            List.of(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36);
    private static final Map<Place, Predicate<Integer>> TESTS = Map.of(
            Place.RED, RED_NUMS::contains,
            Place.BLACK, Predicate.not(RED_NUMS::contains),
            Place.LOW, num -> NumberUtil.isBetween(num, 1, 19),
            Place.HIGH, num -> NumberUtil.isBetween(num, 19, 37),
            Place.EVEN, num -> num % 2 == 0,
            Place.ODD, num -> num % 2 != 0);

    private Instant startTimer;
    private String results;

    public RouletteGame(Context context) {
        super(context, Duration.ofSeconds(30));
    }

    @Override
    public Mono<Void> start() {
        return Mono.fromRunnable(() -> {
            this.schedule(this.end());
            this.startTimer = Instant.now();
        });
    }

    @Override
    public Mono<MessageData> show() {
        return Flux.fromIterable(this.getPlayers().values())
                .flatMap(player -> Mono.zip(Mono.just(player), player.getUsername(this.getContext().getClient())))
                .collectList()
                .map(list -> ShadbotUtil.getDefaultEmbed(
                        embed -> {
                            final String description = String.format("**Use `/%s <bet> <place>` to join the game.**"
                                            + "%n%n**place** is a `number between 1 and 36`, %s",
                                    this.getContext().getCommandName(),
                                    FormatUtil.format(Place.values(),
                                            value -> "`%s`".formatted(value.name().toLowerCase()), ", "));
                            final String desc = FormatUtil.format(list,
                                    TupleUtils.function((player, username) -> String.format("**%s** (%s coin(s))",
                                            username, this.context.localize(player.getBet()))), "\n");
                            final String place = this.getPlayers().values().stream()
                                    .map(RoulettePlayer::getPlace)
                                    .collect(Collectors.joining("\n"));

                            embed.setAuthor("Roulette Game", null, this.getContext().getAuthorAvatar())
                                    .setThumbnail("https://i.imgur.com/D7xZd6C.png")
                                    .setDescription(description)
                                    .addField("Player (Bet)", desc, true)
                                    .addField("Place", place, true);

                            if (this.results != null) {
                                embed.addField("Results", this.results, false);
                            }

                            if (this.isScheduled()) {
                                final Duration remainingDuration = this.getDuration()
                                        .minus(TimeUtil.elapsed(this.startTimer));
                                embed.setFooter(String.format("You have %d seconds to make your bets.",
                                        remainingDuration.toSeconds()), null);
                            } else {
                                embed.setFooter("Finished.", null);
                            }
                        }))
                .flatMap(this.context::editReply);
    }

    @Override
    public Mono<Void> end() {
        final int winningPlace = ThreadLocalRandom.current().nextInt(1, 37);
        return Flux.fromIterable(this.getPlayers().values())
                .flatMap(player -> Mono.zip(Mono.just(player), player.getUsername(this.getContext().getClient())))
                .flatMap(TupleUtils.function((player, username) -> {
                    final Place place = EnumUtil.parseEnum(Place.class, player.getPlace());

                    final int multiplier = RouletteGame.getMultiplier(player, place, winningPlace);
                    if (multiplier > 0) {
                        final long gains = Math.min(player.getBet() * multiplier, Config.MAX_COINS);
                        Telemetry.ROULETTE_SUMMARY.labels("win").observe(gains);
                        return player.win(gains)
                                .thenReturn("**%s** (Gains: **%s coin(s)**)"
                                        .formatted(username, this.context.localize(gains)));
                    } else {
                        Telemetry.ROULETTE_SUMMARY.labels("loss").observe(player.getBet());
                        return Mono.just("**%s** (Losses: **%s coin(s)**)"
                                .formatted(username, this.context.localize(player.getBet())));
                    }
                }))
                .collectSortedList()
                .doOnNext(list -> this.results = String.join(", ", list))
                .then(this.getContext().getChannel())
                .flatMap(channel -> DiscordUtil.sendMessage(
                        String.format(Emoji.DICE + " No more bets. *The wheel is spinning...* **%d (%s)** !",
                                winningPlace, RED_NUMS.contains(winningPlace) ? "Red" : "Black"), channel))
                .then(this.show())
                .then(Mono.fromRunnable(this::destroy));
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

}
