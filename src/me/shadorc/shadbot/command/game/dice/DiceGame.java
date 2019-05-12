package me.shadorc.shadbot.command.game.dice;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.CommandInitializer;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.game.GameCmd;
import me.shadorc.shadbot.core.game.MultiplayerGame;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.MoneyEnum;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.object.message.UpdateableMessage;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class DiceGame extends MultiplayerGame<DicePlayer> {

    private final long bet;
    private final UpdateableMessage updateableMessage;

    private long startTime;
    private String results;

    public DiceGame(GameCmd<DiceGame> gameCmd, Context context, long bet) {
        super(gameCmd, context, Duration.ofSeconds(30));
        this.bet = bet;
        this.updateableMessage = new UpdateableMessage(context.getClient(), context.getChannelId());
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
                    if (tuple.getT1().getNumber() == winningNum) {
                        long gains = Math.min((long) (this.bet * (this.getPlayers().size() + DiceCmd.MULTIPLIER)), Config.MAX_COINS);
                        StatsManager.MONEY_STATS.log(MoneyEnum.MONEY_GAINED, CommandInitializer.getCommand(this.getContext().getCommandName()).getName(), gains);
                        Shadbot.getDatabase().getDBMember(this.getContext().getGuildId(), tuple.getT1().getUserId()).addCoins(gains);
                        return String.format("**%s** (Gains: **%s**)", tuple.getT2(), FormatUtils.coins(gains));
                    } else {
                        StatsManager.MONEY_STATS.log(MoneyEnum.MONEY_LOST, CommandInitializer.getCommand(this.getContext().getCommandName()).getName(), this.bet);
                        Shadbot.getLottery().addToJackpot(this.bet);
                        return String.format("**%s** (Losses: **%s**)", tuple.getT2(), this.bet);
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
                .map(usernames -> EmbedUtils.getDefaultEmbed()
                        .andThen(embed -> {
                            embed.setAuthor("Dice Game", null, this.getContext().getAvatarUrl())
                                    .setThumbnail("http://findicons.com/files/icons/2118/nuvola/128/package_games_board.png")
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
                .flatMap(this.updateableMessage::send)
                .then();
    }

    public long getBet() {
        return this.bet;
    }

}
