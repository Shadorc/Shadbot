package me.shadorc.shadbot.command.currency;

import java.util.List;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.CastUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IUser;

@RateLimited
@Command(category = CommandCategory.CURRENCY, names = { "transfer" })
public class TransferCoinsCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		List<String> splitCmd = StringUtils.split(context.getArg(), 2);
		if(splitCmd.size() != 2) {
			throw new MissingArgumentException();
		}

		IUser receiverUser = context.getMessage().getMentions().get(0);
		IUser senderUser = context.getAuthor();
		if(receiverUser.equals(senderUser)) {
			throw new IllegalArgumentException("You cannot transfer coins to yourself.");
		}

		Integer coins = CastUtils.asPositiveInt(splitCmd.get(0));
		if(coins == null) {
			throw new IllegalArgumentException("Invalid amount.");
		}

		if(Database.getDBUser(context.getGuild(), senderUser).getCoins() < coins) {
			BotUtils.sendMessage(TextUtils.notEnoughCoins(context.getAuthor()), context.getChannel());
			return;
		}

		if(Database.getDBUser(context.getGuild(), receiverUser).getCoins() + coins >= Config.MAX_COINS) {
			BotUtils.sendMessage(Emoji.BANK + " This transfer cannot be done because " + receiverUser.getName()
					+ " would exceed the maximum coins cap.", context.getChannel());
			return;
		}

		Database.getDBUser(context.getGuild(), senderUser).addCoins(-coins);
		Database.getDBUser(context.getGuild(), receiverUser).addCoins(coins);

		BotUtils.sendMessage(String.format(Emoji.BANK + " %s has transfered **%s** to %s",
				senderUser.mention(),
				FormatUtils.formatCoins(coins),
				receiverUser.mention()),
				context.getChannel());
	}

	@Override
	public EmbedObject getHelp(Context context) {
		return new HelpBuilder(this, context.getPrefix())
				.setDescription("Transfer coins to the mentioned user.")
				.addArg("coins", false)
				.addArg("@user", false)
				.build();
	}
}
