package com.shadorc.shadbot.command.game.lottery;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.lottery.entity.LotteryGambler;
import com.shadorc.shadbot.db.lottery.entity.LotteryHistoric;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.List;
import java.util.function.Consumer;

public class LotteryEmbedBuilder {

    private final Context context;
    private Consumer<EmbedCreateSpec> embedConsumer;

    private LotteryEmbedBuilder(Context context) {
        this.context = context;
        this.embedConsumer = DiscordUtils.getDefaultEmbed().andThen(embed ->
                embed.setAuthor("Lottery", null, context.getAvatarUrl())
                        .setThumbnail("https://i.imgur.com/peLGtkS.png")
                        .setDescription(String.format("The next draw will take place in **%s**%nTo " +
                                        "participate, type: `%s%s %d-%d`",
                                FormatUtils.customDate(LotteryCmd.getDelay()),
                                context.getPrefix(), context.getCommandName(), LotteryCmd.MIN_NUM, LotteryCmd.MAX_NUM)));
    }

    public static LotteryEmbedBuilder create(Context context) {
        return new LotteryEmbedBuilder(context);
    }

    public LotteryEmbedBuilder withGamblers(List<LotteryGambler> gamblers) {
        this.embedConsumer = this.embedConsumer.andThen(embed -> embed.addField("Number of participants",
                Integer.toString(gamblers.size()), false));

        gamblers.stream()
                .filter(lotteryGambler -> lotteryGambler.getUserId().equals(this.context.getAuthorId()))
                .findFirst()
                .ifPresent(gambler -> this.embedConsumer = this.embedConsumer.andThen(embed -> embed.setFooter(
                        String.format("You bet on number %d.", gambler.getNumber()),
                        "https://i.imgur.com/btJAaAt.png")));

        return this;
    }

    public LotteryEmbedBuilder withJackpot(long jackpot) {
        this.embedConsumer = this.embedConsumer.andThen(embed -> embed.addField("Prize pool",
                FormatUtils.coins(jackpot), false));
        return this;
    }

    public LotteryEmbedBuilder withHistoric(LotteryHistoric historic) {
        final String people;
        switch (historic.getWinnerCount()) {
            case 0:
                people = "nobody";
                break;
            case 1:
                people = "one person";
                break;
            default:
                people = historic.getWinnerCount() + " people";
                break;
        }

        this.embedConsumer = this.embedConsumer.andThen(embed -> embed.addField("Historic",
                String.format("Last week, the prize pool contained **%s**, the winning " +
                                "number was **%d** and **%s won**.",
                        FormatUtils.coins(historic.getJackpot()), historic.getNumber(), people), false));
        return this;
    }

    public Consumer<EmbedCreateSpec> build() {
        return this.embedConsumer;
    }

}
