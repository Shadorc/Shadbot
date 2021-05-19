package com.locibot.locibot.command.game.roulette;

import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.core.game.MultiplayerGame;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.*;
import com.locibot.locibot.utils.*;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    private static final Map<RouletteCmd.Place, Predicate<Integer>> TESTS = Map.of(
            RouletteCmd.Place.RED, RED_NUMS::contains,
            RouletteCmd.Place.BLACK, Predicate.not(RED_NUMS::contains),
            RouletteCmd.Place.LOW, num -> NumberUtil.isBetween(num, 1, 19),
            RouletteCmd.Place.HIGH, num -> NumberUtil.isBetween(num, 19, 37),
            RouletteCmd.Place.EVEN, num -> num % 2 == 0,
            RouletteCmd.Place.ODD, num -> num % 2 != 0);

    private Instant startTimer;

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
    public Mono<Message> show() {
        return Mono.fromCallable(() -> ShadbotUtil.getDefaultEmbed(
                embed -> {
                    final String description = this.context.localize("roulette.description")
                            .formatted(this.context.getFullCommandName());
                    final String desc = FormatUtil.format(this.players.values(),
                            player -> this.context.localize("roulette.player.field")
                                    .formatted(player.getUsername().orElseThrow(), this.context.localize(player.getBet())), "\n");
                    final String place = this.getPlayers().values().stream()
                            .map(player -> player.getPlace() == RouletteCmd.Place.NUMBER
                                    ? player.getNumber().orElseThrow()
                                    : player.getPlace())
                            .map(Object::toString)
                            .map(StringUtil::capitalize)
                            .collect(Collectors.joining("\n"));

                    embed.setAuthor(this.context.localize("roulette.title"), null, this.context.getAuthorAvatar())
                            .setThumbnail("https://i.imgur.com/D7xZd6C.png")
                            .setDescription(description)
                            .addField(this.context.localize("roulette.player.title"), desc, true)
                            .addField(this.context.localize("roulette.place.title"), place, true);

                    if (this.isScheduled()) {
                        final Duration remainingDuration = this.getDuration()
                                .minus(TimeUtil.elapsed(this.startTimer));
                        embed.setFooter(this.context.localize("roulette.footer.remaining")
                                .formatted(remainingDuration.toSeconds()), null);
                    } else {
                        embed.setFooter(this.context.localize("roulette.footer.finished"), null);
                    }
                }))
                .flatMap(this.context::editFollowupMessage);
    }

    @Override
    public Mono<Void> end() {
        final int winningPlace = ThreadLocalRandom.current().nextInt(1, 37);
        return Flux.fromIterable(this.getPlayers().values())
                .flatMap(player -> {
                    final int multiplier = RouletteGame.getMultiplier(player, winningPlace);
                    if (multiplier > 0) {
                        final long gains = Math.min(player.getBet() * multiplier, Config.MAX_COINS);
                        Telemetry.ROULETTE_SUMMARY.labels("win").observe(gains);
                        return player.win(gains)
                                .thenReturn(this.context.localize("roulette.player.gains")
                                        .formatted(player.getUsername().orElseThrow(), this.context.localize(gains)));
                    } else {
                        Telemetry.ROULETTE_SUMMARY.labels("loss").observe(player.getBet());
                        return Mono.just(this.context.localize("roulette.player.losses")
                                .formatted(player.getUsername().orElseThrow(), this.context.localize(player.getBet())));
                    }
                })
                .collectList()
                .map(list -> String.join("\n", list))
                .flatMap(text -> {
                    final String color = RED_NUMS.contains(winningPlace)
                            ? this.context.localize("roulette.red")
                            : this.context.localize("roulette.black");
                    return this.context.createFollowupMessage(Emoji.DICE, this.context.localize("roulette.results")
                            .formatted(winningPlace, color, text));
                })
                .then(Mono.fromRunnable(this::destroy));
    }

    private static int getMultiplier(RoulettePlayer player, int winningPlace) {
        if (player.getPlace() == RouletteCmd.Place.NUMBER) {
            if (player.getNumber().orElseThrow() == winningPlace) {
                return 36;
            }
            return 0;
        } else if (TESTS.get(player.getPlace()).test(winningPlace)) {
            return 2;
        } else {
            return 0;
        }
    }

}
