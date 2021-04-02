package com.shadorc.shadbot.core.game;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import discord4j.common.util.Snowflake;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.util.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class GameCmd<T extends Game> extends BaseCmd {

    private final Map<Snowflake, T> managers;

    protected GameCmd(String name, String description, @Nullable ApplicationCommandOptionType type) {
        super(CommandCategory.GAME, CommandPermission.USER, name, description, type);
        this.setGameRateLimiter();
        this.managers = new ConcurrentHashMap<>();
    }

    protected GameCmd(String name, String description) {
        this(name, description, null);
    }

    public Map<Snowflake, T> getManagers() {
        return this.managers;
    }
}
