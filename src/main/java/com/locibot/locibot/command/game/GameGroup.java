package com.locibot.locibot.command.game;

import com.locibot.locibot.command.game.dice.DiceCmd;
import com.locibot.locibot.command.game.rps.RpsCmd;
import com.locibot.locibot.command.game.blackjack.BlackjackCmd;
import com.locibot.locibot.command.game.hangman.HangmanCmd;
import com.locibot.locibot.command.game.lottery.LotteryCmd;
import com.locibot.locibot.command.game.roulette.RouletteCmd;
import com.locibot.locibot.command.game.russianroulette.RussianRouletteCmd;
import com.locibot.locibot.command.game.slotmachine.SlotMachineCmd;
import com.locibot.locibot.command.game.trivia.TriviaCmd;
import com.locibot.locibot.core.command.BaseCmdGroup;
import com.locibot.locibot.core.command.CommandCategory;

import java.util.List;

public class GameGroup extends BaseCmdGroup {

    public GameGroup() {
        super(CommandCategory.GAME, "game", "Game commands",
                List.of(new RpsCmd(), new RussianRouletteCmd(), new SlotMachineCmd(), new TriviaCmd(),
                        new HangmanCmd(), new RouletteCmd(), new BlackjackCmd(), new DiceCmd(), new LotteryCmd()));
    }

}
