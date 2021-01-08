package com.shadorc.shadbot.command.currency;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBMember;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.ShadbotUtils;
import discord4j.common.util.Snowflake;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

public class TransferCoinsCmd extends BaseCmd {

    public TransferCoinsCmd() {
        super(CommandCategory.CURRENCY, "transfer", "Transfer coins to a user");
        this.setDefaultRateLimiter();
    }

    @Override
    public ApplicationCommandRequest build(ImmutableApplicationCommandRequest.Builder builder) {
        return builder
                .addOption(ApplicationCommandOptionData.builder()
                        .name("coins")
                        .description("number of coins to transfer")
                        .type(ApplicationCommandOptionType.INTEGER.getValue())
                        .required(true)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("user")
                        .description("user to transfer coins to")
                        .type(ApplicationCommandOptionType.USER.getValue())
                        .required(true)
                        .build())
                .build();
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.acknowledge()
                .then(context.getOptionAsMember("user"))
                .flatMap(receiverUser -> {
                    final Snowflake senderUserId = context.getAuthorId();
                    if (receiverUser.getId().equals(senderUserId)) {
                        return Mono.error(new CommandException("You cannot transfer coins to yourself."));
                    }

                    final int coins = context.getOptionAsInteger("coins").orElseThrow();
                    if (coins <= 0) {
                        return Mono.error(new CommandException(String.format("`%s` is not a valid amount of coins.", coins)));
                    }

                    if (coins > Config.MAX_COINS) {
                        return Mono.error(new CommandException(String.format("You cannot transfer more than %s.",
                                FormatUtils.coins(Config.MAX_COINS))));
                    }

                    return DatabaseManager.getGuilds()
                            .getDBMembers(context.getGuildId(), senderUserId, receiverUser.getId())
                            .collectMap(DBMember::getId)
                            .flatMap(dbMembers -> {
                                final DBMember dbSender = dbMembers.get(senderUserId);
                                if (dbSender.getCoins() < coins) {
                                    return Mono.error(new CommandException(ShadbotUtils.NOT_ENOUGH_COINS));
                                }

                                final DBMember dbReceiver = dbMembers.get(receiverUser.getId());
                                if (dbReceiver.getCoins() + coins >= Config.MAX_COINS) {
                                    return context.createFollowupMessage(
                                            Emoji.BANK + " (**%s**) This transfer cannot be done because %s would " +
                                                    "exceed the maximum coins cap.",
                                            context.getAuthorName(), receiverUser.getUsername());
                                }

                                return dbSender.addCoins(-coins)
                                        .and(dbReceiver.addCoins(coins))
                                        .then(context.createFollowupMessage(
                                                Emoji.BANK + " **%s** has transferred **%s** to **%s**.",
                                                context.getAuthorName(), FormatUtils.coins(coins),
                                                receiverUser.getUsername()));
                            });
                });
    }

}
