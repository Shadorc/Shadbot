package com.locibot.locibot.core.game;

import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import discord4j.common.util.Snowflake;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.util.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class GameCmd<G extends Game> extends BaseCmd implements GameListener {

    private final Map<Snowflake, G> managers;

    protected GameCmd(String name, String description, @Nullable ApplicationCommandOptionType type) {
        super(CommandCategory.GAME, CommandPermission.USER_GUILD, name, description, type);
        this.setGameRateLimiter();
        this.managers = new ConcurrentHashMap<>();
    }

    protected GameCmd(String name, String description) {
        this(name, description, null);
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
