package me.shadorc.discordbot.command.game;

import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Utils;

public class RussianRouletteCmd extends Command {

	public RussianRouletteCmd() {
		super(false, "roulette_russe", "russian_roulette");
	}

	@Override
	public void execute(Context context) {
		if(Utils.rand(6) == 0) {
			BotUtils.sendMessage(":game_die: Une goutte de sueur coule sur votre front, vous pressez la détente... **PAN** ... Désolé, vous êtes mort, vous perdez tous vos gains.", context.getChannel());
			Storage.store(context.getGuild(), context.getAuthor().getLongID(), 0);
		} else {
			BotUtils.sendMessage(":game_die: Une goutte de sueur coule sur votre front, vous pressez la détente... **click** ... Pfiou, vous êtes toujours en vie, vous remportez 50 coins !", context.getChannel());
			Utils.gain(context.getGuild(), context.getAuthor().getLongID(), 50);
		}
	}

}
