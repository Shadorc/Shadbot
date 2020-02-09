package com.shadorc.shadbot.command.currency;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBMember;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import discord4j.core.object.entity.User;
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
        return this.getMentionedUser(context)
                .flatMap(user -> Mono.zip(Mono.just(user),
                        DatabaseManager.getGuilds().getDBMember(context.getGuildId(), user.getId())))
                .map(tuple -> {
                    final User user = tuple.getT1();
                    final DBMember dbMember = tuple.getT2();

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

    private Mono<User> getMentionedUser(Context context) {
        return context.getMessage()
                .getUserMentions()
                .switchIfEmpty(context.getGuild()
                        .flatMapMany(guild -> DiscordUtils.extractMembers(guild, context.getContent())))
                .defaultIfEmpty(context.getAuthor())
                .next();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Show how many coins a user has.")
                .addArg("@user", "if not specified, it will show your coins", true)
                .build();
    }
}