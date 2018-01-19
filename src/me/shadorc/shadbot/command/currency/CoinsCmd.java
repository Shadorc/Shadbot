package me.shadorc.shadbot.command.currency;

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
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IUser;

@RateLimited
@Command(category = CommandCategory.CURRENCY, names = { "coins", "coin" })
public class CoinsCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		String str;
		if(context.getMessage().getMentions().isEmpty()) {
			str = String.format("(**%s**) You have **%s**.",
					context.getAuthorName(), FormatUtils.formatCoins(Database.getDBUser(context.getGuild(), context.getAuthor()).getCoins()));
		} else {
			IUser user = context.getMessage().getMentions().get(0);
			str = String.format("**%s** has **%s**.",
					user.getName(), FormatUtils.formatCoins(Database.getDBUser(context.getGuild(), user).getCoins()));
		}
		BotUtils.sendMessage(Emoji.PURSE + " " + str, context.getChannel());
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show how many coins has an user.")
				.addArg("@user", "if not specified, it will show you your coins", true)
				.build();
	}
}