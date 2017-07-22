package me.shadorc.discordbot.command;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Utils;

public class RussianRouletteCommand extends Command {

	public RussianRouletteCommand() {
		super("roulette_russe");
	}

	@Override
	public void execute(Context context) {
		String author = context.getAuthor().getName();
		if(Utils.rand(6) == 0) {
			BotUtils.sendMessage("Une goutte de sueur coule sur votre front, vous pressez la détente... **PAN** ... Désolé, vous êtes mort, vous perdez tous vos gains.", context.getChannel());
			Storage.store(author, 0);
		} else {
			BotUtils.sendMessage("Une goutte de sueur coule sur votre front, vous pressez la détente... **click** ... Pfiou, vous êtes toujours en vie, vous remportez 50 coins !", context.getChannel());
			Utils.gain(author, 50);
		}
	}

}
