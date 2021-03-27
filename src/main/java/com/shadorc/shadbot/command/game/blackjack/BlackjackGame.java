/*
package com.shadorc.shadbot.command.game.blackjack;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.MultiplayerGame;
import com.shadorc.shadbot.core.ratelimiter.RateLimiter;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.casino.Deck;
import com.shadorc.shadbot.object.casino.Hand;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.ShadbotUtils;
import com.shadorc.shadbot.utils.TimeUtils;
import discord4j.discordjson.json.ImmutableEmbedFieldData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

public class BlackjackGame extends MultiplayerGame<BlackjackCmd, BlackjackPlayer> {

    private final RateLimiter rateLimiter;
    private final UpdatableMessage updatableMessage;

    private final Deck deck;
    private final Hand dealerHand;
    private final Map<String, Consumer<BlackjackPlayer>> actions;

    private long startTime;

    public BlackjackGame(BlackjackCmd gameCmd, Context context) {
        super(gameCmd, context, Duration.ofMinutes(1));

        this.rateLimiter = new RateLimiter(1, Duration.ofSeconds(2));
        this.updatableMessage = new UpdatableMessage(context.getClient(), context.getChannelId());

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
            this.startTime = System.currentTimeMillis();
            BlackjackInputs.create(this.getContext().getClient(), this).listen();
        });
    }

    @Override
    public Mono<Void> end() {
        return Flux.fromIterable(this.getPlayers().values())
                .flatMap(player -> Mono.zip(Mono.just(player), player.getUsername(this.getContext().getClient())))
                .flatMap(TupleUtils.function((player, username) -> {
                    final int dealerValue = this.dealerHand.getValue();
                    final int playerValue = player.getHand().getValue();

                    switch (BlackjackGame.getResult(playerValue, dealerValue)) {
                        case 1:
                            final long coins = Math.min((long) (player.getBet() * Constants.WIN_MULTIPLICATOR), Config.MAX_COINS);
                            Telemetry.BLACKJACK_SUMMARY.labels("win").observe(coins);
                            return player.cancelBet()
                                    .then(player.win(coins))
                                    .thenReturn(String.format("**%s** (Gains: **%s**)", username, FormatUtils.coins(coins)));
                        case -1:
                            Telemetry.BLACKJACK_SUMMARY.labels("loss").observe(player.getBet());
                            return player.cancelBet()
                                    .then(player.lose(player.getBet()))
                                    .thenReturn(String.format("**%s** (Losses: **%s**)",
                                            username, FormatUtils.coins(player.getBet())));
                        default:
                            return player.cancelBet()
                                    .thenReturn(String.format("**%s** (Draw)", username));
                    }
                }))
                .collectList()
                .flatMap(results -> this.getContext().getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(
                                String.format(Emoji.DICE + " __Results:__ %s", String.join(", ", results)), channel)))
                .then(Mono.fromRunnable(this::destroy))
                .then(this.show());
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
    public Mono<Void> show() {
        return Flux.fromIterable(this.getPlayers().values())
                .flatMap(player -> player.format(this.getContext().getClient()))
                .collectList()
                .map(hands -> ShadbotUtils.getDefaultEmbed()
                        .andThen(embed -> {
                            final Hand visibleDealerHand = this.isScheduled() ?
                                    new Hand(this.dealerHand.getCards().subList(0, 1)) : this.dealerHand;
                            embed.setAuthor("Blackjack Game", null, this.getContext().getAvatarUrl())
                                    .setThumbnail("https://i.imgur.com/oESeVrU.png")
                                    .setDescription(String.format("**Use `%s%s <bet>` to join the game.**"
                                                    + "%n%nType `hit` to take another card, `stand` to pass or "
                                                    + "`double down` to double down.",
                                            this.getContext().getPrefix(), this.getContext().getCommandName()))
                                    .addField("Dealer's hand", visibleDealerHand.format(), true);

                            if (this.isScheduled()) {
                                final Duration remainingDuration = this.getDuration()
                                        .minusMillis(TimeUtils.getMillisUntil(this.startTime));
                                embed.setFooter(
                                        String.format("Will automatically stop in %s seconds. Use %scancel to force the stop.",
                                                remainingDuration.toSeconds(), this.getContext().getPrefix()), null);
                            } else {
                                embed.setFooter("Finished", null);
                            }

                            for (final ImmutableEmbedFieldData field : hands) {
                                embed.addField(field.name(), field.value(), field.inline().get());
                            }
                        }))
                .map(this.updatableMessage::setEmbed)
                .flatMap(UpdatableMessage::send)
                .then();
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

}*/
