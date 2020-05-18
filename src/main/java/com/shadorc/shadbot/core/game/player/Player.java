package com.shadorc.shadbot.core.game.player;

import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBMember;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Snowflake;
import reactor.core.publisher.Mono;

public class Player {

    private final Snowflake guildId;
    private final Snowflake userId;

    public Player(Snowflake guildId, Snowflake userId) {
        this.guildId = guildId;
        this.userId = userId;
    }

    public Snowflake getUserId() {
        return this.userId;
    }

    public Mono<String> getUsername(GatewayDiscordClient gateway) {
        return gateway.getUserById(this.userId).map(User::getUsername);
    }

    public Mono<DBMember> getDBMember() {
        return DatabaseManager.getGuilds().getDBMember(this.guildId, this.userId);
    }

    public Mono<Void> win(long coins) {
        return this.getDBMember()
                .flatMap(dbMember -> dbMember.addCoins(coins))
                .then();
    }

    public Mono<Void> lose(long coins) {
        return this.getDBMember()
                .flatMap(dbMember -> dbMember.addCoins(-coins))
                .and(DatabaseManager.getLottery().addToJackpot(coins));
    }

}
