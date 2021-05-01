package com.shadorc.shadbot.core.game;

import discord4j.common.util.Snowflake;

public interface GameListener {

    void onGameDestroy(Snowflake channelId);

}
