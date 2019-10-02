package com.shadorc.shadbot.command.currency;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.database.DBMember;
import com.shadorc.shadbot.db.database.DatabaseManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class CoinsCmd extends BaseCmd {

    public CoinsCmd() {
        super(CommandCategory.CURRENCY, List.of("coins", "coin"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        return context.getMessage()
                .getUserMentions()
                .switchIfEmpty(Mono.just(context.getAuthor()))
                .next()
                .map(user -> {
                    final DBMember dbMember = DatabaseManager.getInstance().getDBMember(context.getGuildId(), user.getId());
                    final String coins = FormatUtils.coins(dbMember.getCoins());
                    if (user.getId().equals(context.getAuthorId())) {
                        return String.format("(**%s**) You have **%s**.", user.getUsername(), coins);
                    } else {
                        return String.format("**%s** has **%s**.", user.getUsername(), coins);
                    }
                })
                .flatMap(text -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(Emoji.PURSE + " " + text, channel)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show how many coins an user has.")
                .addArg("@user", "if not specified, it will show your coins", true)
                .build();
    }
}