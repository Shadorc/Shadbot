package me.shadorc.shadbot.command.game.trivia;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.core.game.Player;

public class TriviaPlayer extends Player {

    private boolean hasAnswered;

    public TriviaPlayer(Snowflake userId) {
        super(userId);
        this.hasAnswered = false;
    }

    public boolean hasAnswered() {
        return this.hasAnswered;
    }

    public void setAnswered(boolean hasAnswered) {
        this.hasAnswered = hasAnswered;
    }

}
