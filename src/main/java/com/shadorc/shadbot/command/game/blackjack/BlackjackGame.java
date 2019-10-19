package com.shadorc.shadbot.command.game.blackjack;

import com.shadorc.shadbot.Config;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.GameCmd;
import com.shadorc.shadbot.core.game.MultiplayerGame;
import com.shadorc.shadbot.core.ratelimiter.RateLimiter;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.casino.Deck;
import com.shadorc.shadbot.object.casino.Hand;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.TimeUtils;
import discord4j.common.json.EmbedFieldEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

public class BlackjackGame extends MultiplayerGame<BlackjackPlayer> {

    private final RateLimiter rateLimiter;
    private final UpdatableMessage updatableMessage;

    private final Deck deck;
    private final Hand dealerHand;
    private final Map<String, Consumer<BlackjackPlayer>> actions;

    private long startTime;

    public BlackjackGame(GameCmd<BlackjackGame> gameCmd, Context context) {
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
            new BlackjackInputs(this.getContext().getClient(), this).subscribe();
        });
    }

    @Override
    public Mono<Void> end() {
        return Flux.fromIterable(this.getPlayers().values())
                .flatMap(player -> Mono.zip(Mono.just(player), player.getUsername(this.getContext().getClient())))
                .map(tuple -> {
                    final BlackjackPlayer player = tuple.getT1();
                    final String username = tuple.getT2();

                    final int dealerValue = this.dealerHand.getValue();
                    final int playerValue = player.getHand().getValue();

                    final StringBuilder text = new StringBuilder();
                    switch (BlackjackGame.getResult(playerValue, dealerValue)) {
                        case 1:
                            player.cancelBet();
                            final long coins = Math.min(player.getBet(), Config.MAX_COINS);
                            player.win(coins);
                            return String.format("**%s** (Gains: **%s**)", username, FormatUtils.coins(coins));
                        case -1:
                            return String.format("**%s** (Losses: **%s**)", username, FormatUtils.coins(player.getBet()));
                        default:
                            player.draw();
                            return String.format("**%s** (Draw)", username);
                    }
                })
                .collectList()
                .flatMap(results -> this.getContext().getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(
                                String.format(Emoji.DICE + " __Results:__ %s", String.join(", ", results)), channel)))
                .then(Mono.fromRunnable(this::stop))
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
                .map(hands -> DiscordUtils.getDefaultEmbed()
                        .andThen(embed -> {
                            final Hand visibleDealerHand = this.isScheduled() ? new Hand(this.dealerHand.getCards().subList(0, 1)) : this.dealerHand;
                            embed.setAuthor("Blackjack Game", null, this.getContext().getAvatarUrl())
                                    .setThumbnail("https://i.imgur.com/oESeVrU.png")
                                    .setDescription(String.format("**Use `%s%s <bet>` to join the game.**"
                                                    + "%n%nType `hit` to take another card, `stand` to pass or `double down` to double down.",
                                            this.getContext().getPrefix(), this.getContext().getCommandName()))
                                    .addField("Dealer's hand", visibleDealerHand.format(), true);

                            if (this.isScheduled()) {
                                final Duration remainingDuration = this.getDuration().minusMillis(TimeUtils.getMillisUntil(this.startTime));
                                embed.setFooter(String.format("Will automatically stop in %s seconds. Use %scancel to force the stop.",
                                        remainingDuration.toSeconds(), this.getContext().getPrefix()), null);
                            } else {
                                embed.setFooter("Finished", null);
                            }

                            for (final EmbedFieldEntity field : hands) {
                                embed.addField(field.getName(), field.getValue(), field.isInline());
                            }
                        }))
                .map(this.updatableMessage::setEmbed)
                .flatMap(UpdatableMessage::send)
                .then();
    }

    @Override
    public boolean addPlayerIfAbsent(BlackjackPlayer player) {
        player.hit(this.deck.pick());
        player.hit(this.deck.pick());
        return super.addPlayerIfAbsent(player);
    }

    public boolean areAllPlayersStanding() {
        return this.getPlayers().values().stream().allMatch(BlackjackPlayer::isStanding);
    }

    public RateLimiter getRateLimiter() {
        return this.rateLimiter;
    }

    public Map<String, Consumer<BlackjackPlayer>> getActions() {
        return Collections.unmodifiableMap(this.actions);
    }

}