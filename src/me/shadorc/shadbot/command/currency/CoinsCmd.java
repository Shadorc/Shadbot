package me.shadorc.shadbot.command.currency;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.database.DBMember;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
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
                    final DBMember dbMember = Shadbot.getDatabase().getDBMember(context.getGuildId(), user.getId());
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