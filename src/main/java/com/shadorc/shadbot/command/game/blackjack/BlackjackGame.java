package com.shadorc.shadbot.command.game.blackjack;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.MultiplayerGame;
import com.shadorc.shadbot.core.ratelimiter.RateLimiter;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.casino.Deck;
import com.shadorc.shadbot.object.casino.Hand;
import com.shadorc.shadbot.utils.ShadbotUtil;
import com.shadorc.shadbot.utils.TimeUtil;
import discord4j.discordjson.json.MessageData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;

public class BlackjackGame extends MultiplayerGame<BlackjackPlayer> {

    private final RateLimiter rateLimiter;

    private final Deck deck;
    private final Hand dealerHand;
    private final Map<String, Consumer<BlackjackPlayer>> actions;

    private Instant startTimer;

    public BlackjackGame(Context context) {
        super(context, Duration.ofMinutes(1));

        this.rateLimiter = new RateLimiter(1, Duration.ofSeconds(2));

        this.deck = new Deck();
        this.deck.shuffle();
        this.dealerHand = new Hand();

        this.actions = Map.of("hit", player -> player.hit(this.deck.pick()),
                "double down", player -> player.doubleDown(this.deck.pick()),
                "stand", BlackjackPlayer::stand);
    }

    @Override
    public Mono<Void> start() {
        return Mono.fromRunnable(() -> {
            this.dealerHand.deal(this.deck.pick(2));
            while (this.dealerHand.getValue() < 17) {
                this.dealerHand.deal(this.deck.pick());
            }

            this.schedule(this.end());
            this.startTimer = Instant.now();
            BlackjackInputs.create(this.getContext().getClient(), this).listen();
        });
    }

    @Override
    public Mono<MessageData> show() {
        return Mono.
                fromCallable(() -> ShadbotUtil.getDefaultEmbed(embed -> {
                    final Hand visibleDealerHand = this.isScheduled() ?
                            new Hand(this.dealerHand.getCards().subList(0, 1)) : this.dealerHand;
                    embed.setAuthor(this.context.localize("blackjack.title"), null, this.getContext().getAuthorAvatar())
                            .setThumbnail("https://i.imgur.com/oESeVrU.png")
                            .setDescription(this.context.localize("blackjack.description")
                                    .formatted(this.context.getFullCommandName()))
                            .addField(this.context.localize("blackjack.dealer.hand"), visibleDealerHand.format(), true);

                    if (this.isScheduled()) {
                        final Duration remainingDuration = this.getDuration().minus(TimeUtil.elapsed(this.startTimer));
                        embed.setFooter(this.context.localize("blackjack.footer.remaining")
                                .formatted(remainingDuration.toSeconds()), null);
                    } else {
                        embed.setFooter(this.context.localize("blackjack.footer.finished"), null);
                    }

                    this.players.values().stream()
                            .map(player -> player.format(this.context.getLocale()))
                            .forEach(field -> embed.addField(field.name(), field.value(), field.inline().get()));
                }))
                .flatMap(this.context::editReply);
    }

    @Override
    public Mono<Void> end() {
        return Flux.fromIterable(this.getPlayers().values())
                .flatMap(player -> {
                    final int dealerValue = this.dealerHand.getValue();
                    final int playerValue = player.getHand().getValue();

                    switch (BlackjackGame.getResult(playerValue, dealerValue)) {
                        case 1:
                            final long coins = Math.min((long) (player.getBet() * Constants.WIN_MULTIPLICATOR), Config.MAX_COINS);
                            Telemetry.BLACKJACK_SUMMARY.labels("win").observe(coins);
                            return player.cancelBet()
                                    .then(player.win(coins))
                                    .thenReturn(this.context.localize("blackjack.gains")
                                            .formatted(player.getUsername(), this.context.localize(coins)));
                        case -1:
                            Telemetry.BLACKJACK_SUMMARY.labels("loss").observe(player.getBet());
                            return player.cancelBet()
                                    .then(player.lose(player.getBet()))
                                    .thenReturn(this.context.localize("blackjack.losses")
                                            .formatted(player.getUsername(), this.context.localize(player.getBet())));
                        default:
                            return player.cancelBet()
                                    .thenReturn(this.context.localize("blackjack.draw").formatted(player.getUsername()));
                    }
                })
                .collectList()
                .map(results -> String.join("\n", results))
                .flatMap(text -> this.context.reply(Emoji.DICE, this.context.localize("blackjack.results")
                        .formatted(text)))
                .then(this.show())
                .then(Mono.fromRunnable(this::destroy));
    }

    // -1 = Lose | 0 = Draw | 1 = Win
    private static int getResult(int playerValue, int dealerValue) {
        if (playerValue > 21) {
            return -1;
        } else if (dealerValue <= 21) {
            return Integer.compare(playerValue, dealerValue);
        } else {
            return 1;
        }
    }

    @Override
    public boolean addPlayerIfAbsent(BlackjackPlayer player) {
        if (super.addPlayerIfAbsent(player)) {
            player.hit(this.deck.pick());
            player.hit(this.deck.pick());
            return true;
        }
        return false;
    }

    public boolean areAllPlayersStanding() {
        return this.getPlayers().values().stream().allMatch(BlackjackPlayer::isStanding);
    }

    public RateLimiter getRateLimiter() {
        return this.rateLimiter;
    }

    public Map<String, Consumer<BlackjackPlayer>> getActions() {
        return this.actions;
    }

}
