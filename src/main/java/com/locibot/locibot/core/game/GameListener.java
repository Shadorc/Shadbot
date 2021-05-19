package com.locibot.locibot.core.game;

import discord4j.common.util.Snowflake;

public interface GameListener {

    void onGameDestroy(Snowflake channelId);

}
