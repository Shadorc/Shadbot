package com.shadorc.shadbot.command.game.lottery;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.database.lottery.entity.LotteryGambler;
import com.shadorc.shadbot.database.lottery.entity.LotteryHistoric;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.List;

public class LotteryEmbedBuilder {

    private final Context context;
    private final EmbedCreateSpec.Builder embed;

    private LotteryEmbedBuilder(Context context) {
        this.context = context;
        this.embed = ShadbotUtil.createEmbedBuilder()
                .author(context.localize("lottery.embed.title"), null, context.getAuthorAvatar())
                .thumbnail("https://i.imgur.com/peLGtkS.png")
                .description(context.localize("lottery.embed.description")
                        .formatted(FormatUtil.formatDurationWords(context.getLocale(), LotteryCmd.getDelay()),
                                context.getCommandName(), Constants.MIN_NUM, Constants.MAX_NUM));
    }

    public static LotteryEmbedBuilder create(Context context) {
        return new LotteryEmbedBuilder(context);
    }

    public LotteryEmbedBuilder withGamblers(List<LotteryGambler> gamblers) {
        this.embed.addField(this.context.localize("lottery.embed.participants"),
                this.context.localize(gamblers.size()), false);

        gamblers.stream()
                .filter(lotteryGambler -> lotteryGambler.getUserId().equals(this.context.getAuthorId()))
                .findFirst()
                .ifPresent(gambler -> this.embed.footer(this.context.localize("lottery.embed.bet")
                                .formatted(gambler.getNumber()),
                        "https://i.imgur.com/btJAaAt.png"));

        return this;
    }

    public LotteryEmbedBuilder withJackpot(long jackpot) {
        this.embed.addField(this.context.localize("lottery.embed.pool.title"),
                this.context.localize("lottery.embed.pool.coins")
                        .formatted(this.context.localize(jackpot)), false);
        return this;
    }

    public LotteryEmbedBuilder withHistoric(LotteryHistoric historic) {
        final String people = switch (historic.getWinnerCount()) {
            case 0 -> this.context.localize("lottery.nobody");
            case 1 -> this.context.localize("lottery.one.person");
            default -> this.context.localize("lottery.people")
                    .formatted(this.context.localize(historic.getWinnerCount()));
        };

        this.embed.addField(this.context.localize("lottery.embed.historic.title"),
                this.context.localize("lottery.embed.historic.description")
                        .formatted(this.context.localize(historic.getJackpot()), historic.getNumber(), people),
                false);
        return this;
    }

    public EmbedCreateSpec build() {
        return this.embed.build();
    }

}
