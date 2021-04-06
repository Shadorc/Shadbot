package com.shadorc.shadbot.command.game;

import com.shadorc.shadbot.command.game.blackjack.BlackjackCmd;
import com.shadorc.shadbot.command.game.dice.DiceCmd;
import com.shadorc.shadbot.command.game.hangman.HangmanCmd;
import com.shadorc.shadbot.command.game.lottery.LotteryCmd;
import com.shadorc.shadbot.command.game.roulette.RouletteCmd;
import com.shadorc.shadbot.command.game.rps.RpsCmd;
import com.shadorc.shadbot.command.game.russianroulette.RussianRouletteCmd;
import com.shadorc.shadbot.command.game.slotmachine.SlotMachineCmd;
import com.shadorc.shadbot.command.game.trivia.TriviaCmd;
import com.shadorc.shadbot.core.command.BaseCmdGroup;
import com.shadorc.shadbot.core.command.CommandCategory;

import java.util.List;

public class GameGroup extends BaseCmdGroup {

    public GameGroup() {
        super(CommandCategory.GAME, "game", "Game commands",
                List.of(new RpsCmd(), new RussianRouletteCmd(), new SlotMachineCmd(), new TriviaCmd(),
                        new HangmanCmd(), new RouletteCmd(), new BlackjackCmd(), new DiceCmd(), new LotteryCmd()));
    }

}
