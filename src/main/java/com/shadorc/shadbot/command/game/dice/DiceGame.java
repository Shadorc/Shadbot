package com.shadorc.shadbot.command.game.dice;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.MultiplayerGame;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import com.shadorc.shadbot.utils.TimeUtil;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

public class DiceGame extends MultiplayerGame<DicePlayer> {

    private final long bet;
    private Instant startTimer;

    public DiceGame(Context context, long bet) {
        super(context, Duration.ofSeconds(30));
        this.bet = bet;
    }

    @Override
    public Mono<Void> start() {
        return Mono.fromRunnable(() -> {
            this.schedule(this.end());
            this.startTimer = Instant.now();
        });
    }

    @Override
    public Mono<Void> end() {
        final int winningNum = ThreadLocalRandom.current().nextInt(1, 7);
        return Flux.fromIterable(this.getPlayers().values())
                .flatMap(player -> {
                    if (player.getNumber() == winningNum) {
                        final long gains = Math.min((long) (this.bet * (this.getPlayers().size() + Constants.WIN_MULTIPLICATOR)),
                                Config.MAX_COINS);
                        Telemetry.DICE_SUMMARY.labels("win").observe(gains);
                        return player.win(gains)
                                .thenReturn(this.context.localize("dice.player.gains")
                                        .formatted(player.getUsername().orElseThrow(), this.context.localize(gains)));
                    } else {
                        Telemetry.DICE_SUMMARY.labels("loss").observe(this.bet);
                        return Mono.just(this.context.localize("dice.player.losses")
                                .formatted(player.getUsername().orElseThrow(), this.context.localize(this.bet)));
                    }
                })
                .collectList()
                .map(results -> String.join("\n", results))
                .flatMap(text -> this.context.reply(Emoji.DICE, this.context.localize("dice.results")
                        .formatted(winningNum, text)))
                .then(Mono.fromRunnable(this::destroy));
    }

    @Override
    public Mono<Message> show() {
        return Mono.
                fromCallable(() -> ShadbotUtil.getDefaultEmbed(embed -> {
                    embed.setAuthor(this.context.localize("dice.title"), null, this.getContext().getAuthorAvatar())
                            .setThumbnail("https://i.imgur.com/XgOilIW.png")
                            .setDescription(this.context.localize("dice.description")
                                    .formatted(this.context.getCommandName(), this.context.getSubCommandGroupName().orElseThrow(),
                                            DiceCmd.JOIN_SUB_COMMAND, this.context.localize(this.bet)))
                            .addField(this.context.localize("dice.player.title"), FormatUtil.format(this.players.values(),
                                    player -> player.getUsername().orElseThrow(), "\n"), true)
                            .addField(this.context.localize("dice.number.title"), FormatUtil.format(this.players.values(),
                                    player -> Integer.toString(player.getNumber()), "\n"), true);

                    if (this.isScheduled()) {
                        final Duration remainingDuration = this.getDuration().minus(TimeUtil.elapsed(this.startTimer));
                        embed.setFooter(this.context.localize("dice.footer.remaining")
                                .formatted(remainingDuration.toSeconds()), null);
                    } else {
                        embed.setFooter(this.context.localize("dice.footer.finished"), null);
                    }
                }))
                .flatMap(this.context::editReply);
    }

    public long getBet() {
        return this.bet;
    }

}
