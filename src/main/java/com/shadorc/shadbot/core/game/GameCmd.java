package com.shadorc.shadbot.core.game;

import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.GroupCmd;
import com.shadorc.shadbot.core.command.SubCmd;
import discord4j.common.util.Snowflake;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.util.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class GameCmd<G extends Game> extends SubCmd implements GameListener {

    private final Map<Snowflake, G> managers;

    protected GameCmd(GroupCmd groupCmd, String name, String description, @Nullable ApplicationCommandOptionType type) {
        super(groupCmd, CommandCategory.GAME, CommandPermission.USER, name, description, type);
        this.setGameRateLimiter();
        this.managers = new ConcurrentHashMap<>();
    }

    protected GameCmd(GroupCmd groupCmd, String name, String description) {
        this(groupCmd, name, description, null);
    }

    public G getGame(Snowflake channelId) {
        return this.managers.get(channelId);
    }

    public void addGame(Snowflake channelId, G game) {
        this.managers.put(channelId, game);
        game.addGameListener(this);
    }

    public void removeGame(Snowflake channelId) {
        this.managers.remove(channelId);
    }

    public boolean isGameStarted(Snowflake channelId) {
        return this.managers.containsKey(channelId);
    }

    @Override
    public void onGameDestroy(Snowflake channelId) {
        this.removeGame(channelId);
    }
}
