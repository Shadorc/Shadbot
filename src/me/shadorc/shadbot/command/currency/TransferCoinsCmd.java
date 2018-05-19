package me.shadorc.shadbot.command.currency;

import java.util.List;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;

@RateLimited
@Command(category = CommandCategory.CURRENCY, names = { "transfer" })
public class TransferCoinsCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		context.requireArg();

		if(!context.getMessage().getUserMentions().hasElements().block()) {
			throw new MissingArgumentException();
		}

		List<String> splitCmd = StringUtils.split(context.getArg().get(), 2);
		if(splitCmd.size() != 2) {
			throw new MissingArgumentException();
		}

		User receiverUser = context.getMessage().getUserMentions().blockFirst();
		User senderUser = context.getAuthor();
		if(receiverUser.equals(senderUser)) {
			throw new IllegalCmdArgumentException("You cannot transfer coins to yourself.");
		}

		Integer coins = NumberUtils.asPositiveInt(splitCmd.get(0));
		if(coins == null) {
			throw new IllegalCmdArgumentException(
					String.format("`%s` is not a valid amount for coins.", splitCmd.get(0)));
		}

		Guild guild = context.getGuild().get();

		if(Database.getDBUser(guild, senderUser).getCoins() < coins) {
			BotUtils.sendMessage(TextUtils.notEnoughCoins(context.getAuthor()), context.getChannel());
			return;
		}

		if(Database.getDBUser(guild, receiverUser).getCoins() + coins >= Config.MAX_COINS) {
			BotUtils.sendMessage(String.format(Emoji.BANK + " This transfer cannot be done because "
					+ "%s would exceed the maximum coins cap.",
					receiverUser.getUsername()), context.getChannel());
			return;
		}

		Database.getDBUser(guild, senderUser).addCoins(-coins);
		Database.getDBUser(guild, receiverUser).addCoins(coins);

		BotUtils.sendMessage(String.format(Emoji.BANK + " %s has transfered **%s** to %s",
				senderUser.getMention(),
				FormatUtils.formatCoins(coins),
				receiverUser.getMention()), context.getChannel());
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Transfer coins to the mentioned user.")
				.addArg("coins", false)
				.addArg("@user", false)
				.build();
	}
}
