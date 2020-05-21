package com.shadorc.shadbot.core.game;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import discord4j.common.util.Snowflake;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class GameCmd<T extends Game<?>> extends BaseCmd {

    private final Map<Snowflake, T> managers;

    protected GameCmd(List<String> names, String alias) {
        super(CommandCategory.GAME, names, alias);
        this.setGameRateLimiter();
        this.managers = new ConcurrentHashMap<>();
    }

    protected GameCmd(List<String> names) {
        this(names, null);
    }

    public Map<Snowflake, T> getManagers() {
        return this.managers;
    }
}
