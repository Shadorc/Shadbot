package com.shadorc.shadbot.command.game.dice;

import com.shadorc.shadbot.Config;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.GameCmd;
import com.shadorc.shadbot.core.game.MultiplayerGame;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.TimeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class DiceGame extends MultiplayerGame<DicePlayer> {

    private final long bet;
    private final UpdatableMessage updatableMessage;

    private long startTime;
    private String results;

    public DiceGame(GameCmd<DiceGame> gameCmd, Context context, long bet) {
        super(gameCmd, context, Duration.ofSeconds(30));
        this.bet = bet;
        this.updatableMessage = new UpdatableMessage(context.getClient(), context.getChannelId());
    }

    @Override
    public void start() {
        this.schedule(this.end());
        this.startTime = System.currentTimeMillis();
        new DiceInputs(this.getContext().getClient(), this).subscribe();
    }

    @Override
    public Mono<Void> end() {
        final int winningNum = ThreadLocalRandom.current().nextInt(1, 7);
        return Flux.fromIterable(this.getPlayers().values())
                .flatMap(player -> Mono.zip(Mono.just(player), player.getUsername(this.getContext().getClient())))
                .map(tuple -> {
                    final DicePlayer player = tuple.getT1();
                    final String username = tuple.getT2();
                    if (player.getNumber() == winningNum) {
                        long gains = Math.min((long) (this.bet * (this.getPlayers().size() + DiceCmd.MULTIPLIER)), Config.MAX_COINS);
                        player.win(gains);
                        return String.format("**%s** (Gains: **%s**)", username, FormatUtils.coins(gains));
                    } else {
                        return String.format("**%s** (Losses: **%s**)", username, this.bet);
                    }

                })
                .collectList()
                .map(list -> this.results = String.join("\n", list))
                .then(this.getContext().getChannel())
                .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.DICE + " The dice is rolling... **%s** !", winningNum), channel))
                .then(this.show())
                .then(Mono.fromRunnable(this::stop));
    }

    @Override
    public Mono<Void> show() {
        return Flux.fromIterable(this.getPlayers().values())
                .flatMap(player -> player.getUsername(this.getContext().getClient()))
                .collectList()
                .map(usernames -> DiscordUtils.getDefaultEmbed()
                        .andThen(embed -> {
                            embed.setAuthor("Dice Game", null, this.getContext().getAvatarUrl())
                                    .setThumbnail("https://i.imgur.com/XgOilIW.png")
                                    .setDescription(String.format("**Use `%s%s <num>` to join the game.**%n**Bet:** %s",
                                            this.getContext().getPrefix(), this.getContext().getCommandName(), FormatUtils.coins(this.bet)))
                                    .addField("Player", String.join("\n", usernames), true)
                                    .addField("Number", this.getPlayers().values().stream()
                                            .map(DicePlayer::getNumber)
                                            .map(Object::toString)
                                            .collect(Collectors.joining("\n")), true);

                            if (this.results != null) {
                                embed.addField("Results", this.results, false);
                            }

                            if (this.isScheduled()) {
                                final Duration remainingDuration = this.getDuration().minusMillis(TimeUtils.getMillisUntil(this.startTime));
                                embed.setFooter(String.format("You have %d seconds to make your bets. Use %scancel to force the stop.",
                                        remainingDuration.toSeconds(), this.getContext().getPrefix()), null);
                            } else {
                                embed.setFooter("Finished.", null);
                            }
                        }))
                .flatMap(this.updatableMessage::send)
                .then();
    }

    public long getBet() {
        return this.bet;
    }

}
