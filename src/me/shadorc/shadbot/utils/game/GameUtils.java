package me.shadorc.shadbot.utils.game;

import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.CastUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;

public class GameUtils {

	public static Integer checkAndGetBet(IChannel channel, IUser user, String betStr, int maxValue) {
		Integer bet = CastUtils.asPositiveInt(betStr);
		if(bet == null) {
			return null;
		}

		if(Database.getDBUser(channel.getGuild(), user).getCoins() < bet) {
			BotUtils.sendMessage(TextUtils.notEnoughCoins(user), channel);
			return null;
		}

		if(bet > maxValue) {
			BotUtils.sendMessage(String.format(Emoji.BANK + " Sorry, you can't bet more than **%s**.",
					FormatUtils.formatCoins(maxValue)), channel);
			return null;
		}

		return bet;
	}
}
