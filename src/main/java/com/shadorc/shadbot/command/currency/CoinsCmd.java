package com.shadorc.shadbot.command.currency;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBMember;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.FormatUtils;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

public class CoinsCmd extends BaseCmd {

    public CoinsCmd() {
        super(CommandCategory.CURRENCY, "coins", "Show how many coins a user has");
        this.setDefaultRateLimiter();
    }

    @Override
    public ApplicationCommandRequest build(ImmutableApplicationCommandRequest.Builder builder) {
        return builder
                .addOption(ApplicationCommandOptionData.builder()
                        .name("user")
                        .description("show your coins by default")
                        .type(ApplicationCommandOptionType.USER.getValue())
                        .required(false)
                        .build())
                .build();
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.acknowledge()
                .then(context.getOptionAsMember("user")
                        .defaultIfEmpty(context.getAuthor()))
                .flatMap(user -> DatabaseManager.getGuilds()
                        .getDBMember(context.getGuildId(), user.getId())
                        .map(DBMember::getCoins)
                        .map(FormatUtils::coins)
                        .map(coins -> {
                            if (user.getId().equals(context.getAuthorId())) {
                                return String.format("(**%s**) You have **%s**.", user.getUsername(), coins);
                            } else {
                                return String.format("**%s** has **%s**.", user.getUsername(), coins);
                            }
                        }))
                .flatMap(text -> context.createFollowupMessage(Emoji.PURSE + " " + text));
    }

}