package com.shadorc.shadbot.command.currency;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBMember;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.FormatUtil;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

public class CoinsCmd extends BaseCmd {

    public CoinsCmd() {
        super(CommandCategory.CURRENCY, "coins", "Show how many coins a user has");
        this.addOption("user", "If not specified, it will show your coins", false,
                ApplicationCommandOptionType.USER);
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.getOptionAsMember("user")
                .defaultIfEmpty(context.getAuthor())
                .flatMap(user -> DatabaseManager.getGuilds()
                        .getDBMember(context.getGuildId(), user.getId())
                        .map(DBMember::getCoins)
                        .map(FormatUtil::coins)
                        .map(coins -> {
                            if (user.getId().equals(context.getAuthorId())) {
                                return Emoji.PURSE + " (**%s**) You have **%s**."
                                        .formatted(user.getUsername(), coins);
                            } else {
                                return Emoji.PURSE + " (**%s**) **%s** has **%s**."
                                        .formatted(context.getAuthorName(), user.getUsername(), coins);
                            }
                        }))
                .flatMap(context::createFollowupMessage);
    }

}