package com.shadorc.shadbot.command.game.hangman;

import com.shadorc.shadbot.core.game.player.Player;
import discord4j.common.util.Snowflake;

public class HangmanPlayer extends Player {

    public HangmanPlayer(Snowflake guildId, Snowflake userId) {
        super(guildId, userId);
    }

}
