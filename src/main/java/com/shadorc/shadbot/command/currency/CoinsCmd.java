package com.shadorc.shadbot.command.currency;

import com.shadorc.shadbot.core.command.Cmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.database.DatabaseManager;
import com.shadorc.shadbot.database.guilds.entity.DBMember;
import com.shadorc.shadbot.object.Emoji;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

public class CoinsCmd extends Cmd {

    public CoinsCmd() {
        super(CommandCategory.CURRENCY, "coins", "Show user's coins");
        this.addOption(option -> option.name("user")
                .description("If not specified, it will show your coins")
                .required(false)
                .type(ApplicationCommandOptionType.USER.getValue()));
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.getOptionAsMember("user")
                .defaultIfEmpty(context.getAuthor())
                .flatMap(user -> DatabaseManager.getGuilds()
                        .getDBMember(context.getGuildId(), user.getId())
                        .map(DBMember::getCoins)
                        .flatMap(coins -> {
                            final StringBuilder stringBuilder = new StringBuilder();
                            if (user.getId().equals(context.getAuthorId())) {
                                stringBuilder.append(context.localize("coins.yours")
                                        .formatted(context.localize(coins)));
                            } else {
                                stringBuilder.append(context.localize("coins.user")
                                        .formatted(user.getUsername(), context.localize(coins)));
                            }
                            if (coins == Config.MAX_COINS) {
                                stringBuilder.append(" ")
                                        .append(context.localize("coins.max.reached"));
                            }
                            return context.createFollowupMessage(Emoji.PURSE, stringBuilder.toString());
                        }));
    }

}