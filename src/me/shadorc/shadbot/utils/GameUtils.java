package me.shadorc.shadbot.utils;

import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;

public class GameUtils {

	public static Integer checkAndGetBet(IChannel channel, IUser user, String betStr, int maxValue) throws IllegalCmdArgumentException {
		Integer bet = CastUtils.asPositiveInt(betStr);
		if(bet == null) {
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid amount.", betStr));
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
