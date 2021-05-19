package com.locibot.locibot.command.game.trivia;

import com.locibot.locibot.core.game.player.Player;
import discord4j.common.util.Snowflake;

public class TriviaPlayer extends Player {

    private boolean hasAnswered;

    public TriviaPlayer(Snowflake guildId, Snowflake userId) {
        super(guildId, userId);
        this.hasAnswered = false;
    }

    public boolean hasAnswered() {
        return this.hasAnswered;
    }

    public void setAnswered(boolean hasAnswered) {
        this.hasAnswered = hasAnswered;
    }

}
