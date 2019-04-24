package me.shadorc.shadbot.core.game;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.core.command.Context;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class MultiplayerGame<P extends Player> extends Game {

    private final Map<Snowflake, P> players;

    public MultiplayerGame(GameCmd<?> gameCmd, Context context, Duration duration) {
        super(gameCmd, context, duration);
        this.players = new ConcurrentHashMap<>();
    }

    public Map<Snowflake, P> getPlayers() {
        return Collections.unmodifiableMap(this.players);
    }

    public boolean addPlayerIfAbsent(P player) {
        return this.players.putIfAbsent(player.getUserId(), player) == null;
    }

}
