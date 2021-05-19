package com.locibot.locibot.core.game.player;

import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.guilds.entity.DBMember;
import discord4j.common.util.Snowflake;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.util.Optional;

public class Player {

    protected final Snowflake guildId;
    protected final Snowflake userId;
    @Nullable
    protected final String username;

    public Player(Snowflake guildId, Snowflake userId, @Nullable String username) {
        this.guildId = guildId;
        this.userId = userId;
        this.username = username;
    }

    public Player(Snowflake guildId, Snowflake userId) {
        this(guildId, userId, null);
    }

    public Snowflake getUserId() {
        return this.userId;
    }

    public Optional<String> getUsername() {
        return Optional.ofNullable(this.username);
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
