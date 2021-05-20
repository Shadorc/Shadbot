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
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.GroupCmd;

public class GameGroupCmd extends GroupCmd {

    public GameGroupCmd() {
        super(CommandCategory.GAME, "Game commands");
        this.addSubCommand(new RpsCmd(this));
        this.addSubCommand(new RussianRouletteCmd(this));
        this.addSubCommand(new SlotMachineCmd(this));
        this.addSubCommand(new TriviaCmd(this));
        this.addSubCommand(new HangmanCmd(this));
        this.addSubCommand(new RouletteCmd(this));
        this.addSubCommand(new BlackjackCmd(this));
        this.addSubCommand(new DiceCmd(this));
        this.addSubCommand(new LotteryCmd(this));
    }

}
