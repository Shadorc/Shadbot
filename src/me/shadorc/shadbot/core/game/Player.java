package me.shadorc.shadbot.core.game;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

public class Player {

    private final Snowflake userId;

    public Player(Snowflake userId) {
        this.userId = userId;
    }

    public Snowflake getUserId() {
        return this.userId;
    }

    public Mono<String> getUsername(DiscordClient client) {
        return client.getUserById(this.userId).map(User::getUsername);
    }

}
