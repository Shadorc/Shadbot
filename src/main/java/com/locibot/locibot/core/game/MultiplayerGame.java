package com.locibot.locibot.core.game;

import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.core.game.player.Player;
import discord4j.common.util.Snowflake;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class MultiplayerGame<P extends Player> extends Game {

    protected final Map<Snowflake, P> players;

    protected MultiplayerGame(Context context, Duration duration) {
        super(context, duration);
        this.players = new ConcurrentHashMap<>();
    }

    public Map<Snowflake, P> getPlayers() {
        return Collections.unmodifiableMap(this.players);
    }

    public boolean addPlayerIfAbsent(P player) {
        return this.players.putIfAbsent(player.getUserId(), player) == null;
    }

}
