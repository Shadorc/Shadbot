package com.shadorc.shadbot.command.currency;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBMember;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.common.util.Snowflake;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

class TransferCoinsCmd extends BaseCmd {

    public TransferCoinsCmd() {
        super(CommandCategory.CURRENCY, "transfer", "Transfer coins to a user");
        this.addOption("coins", "Number of coins to transfer", true, ApplicationCommandOptionType.INTEGER);
        this.addOption("user", "User to transfer coins to", true, ApplicationCommandOptionType.USER);
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.getOptionAsMember("user")
                .flatMap(receiverUser -> {
                    final Snowflake senderUserId = context.getAuthorId();
                    if (receiverUser.getId().equals(senderUserId)) {
                        return Mono.error(new CommandException(context.localize("transfer.exception.self")));
                    }

                    final long coins = context.getOptionAsLong("coins").orElseThrow();
                    if (coins <= 0) {
                        return Mono.error(new CommandException(context.localize("transfer.invalid.coins")
                                .formatted(coins)));
                    }

                    if (coins > Config.MAX_COINS) {
                        return Mono.error(new CommandException(context.localize("transfer.exception.max")
                                .formatted(context.localize(Config.MAX_COINS))));
                    }

                    return DatabaseManager.getGuilds()
                            .getDBMembers(context.getGuildId(), senderUserId, receiverUser.getId())
                            .collectMap(DBMember::getId)
                            .flatMap(dbMembers -> {
                                final DBMember dbSender = dbMembers.get(senderUserId);
                                if (dbSender.getCoins() < coins) {
                                    return Mono.error(new CommandException(ShadbotUtil.NOT_ENOUGH_COINS));
                                }

                                final DBMember dbReceiver = dbMembers.get(receiverUser.getId());
                                if (dbReceiver.getCoins() + coins >= Config.MAX_COINS) {
                                    return context.reply(Emoji.BANK, context.localize("transfer.exception.exceeds")
                                            .formatted(receiverUser.getUsername()));
                                }

                                return dbSender.addCoins(-coins)
                                        .and(dbReceiver.addCoins(coins))
                                        .then(context.reply(Emoji.BANK, context.localize("transfer.message")
                                                .formatted(context.localize(coins), receiverUser.getUsername())));
                            });
                });
    }

}
