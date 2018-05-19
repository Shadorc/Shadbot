package me.shadorc.shadbot.command.currency;

import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;

@RateLimited
@Command(category = CommandCategory.CURRENCY, names = { "coins", "coin" })
public class CoinsCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		String text;
		if(!context.getMessage().getUserMentions().hasElements().block()) {
			text = String.format("(**%s**) You have **%s**.",
					context.getUsername(),
					FormatUtils.formatCoins(Database.getDBUser(context.getGuild().get(), context.getAuthor()).getCoins()));
		} else {
			User user = context.getMessage().getUserMentions().blockFirst();
			text = String.format("**%s** has **%s**.",
					user.getUsername(),
					FormatUtils.formatCoins(Database.getDBUser(context.getGuild().get(), user).getCoins()));
		}

		BotUtils.sendMessage(Emoji.PURSE + " " + text, context.getChannel());
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show how many coins has an user.")
				.addArg("@user", "if not specified, it will show you your coins", true)
				.build();
	}
}