package com.shadorc.shadbot.core.game;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.player.Player;
import discord4j.common.util.Snowflake;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class MultiplayerGame<G extends GameCmd<?>, P extends Player> extends Game<G> {

    protected final Map<Snowflake, P> players;

    protected MultiplayerGame(G gameCmd, Context context, Duration duration) {
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
