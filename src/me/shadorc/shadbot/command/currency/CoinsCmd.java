package me.shadorc.shadbot.command.currency;

import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.db.DBMember;
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
		context.getMessage().getUserMentions()
				.single()
				.switchIfEmpty(context.getAuthor())
				.subscribe(user -> this.execute(context, user));

	}

	private void execute(Context context, User user) {
		DBMember dbMember = Database.getDBMember(context.getGuildId().get(), user.getId());
		String coins = FormatUtils.formatCoins(dbMember.getCoins());
		String text;
		if(user.getId().equals(context.getAuthorId())) {
			text = String.format("(**%s**) You have **%s**.", user.getUsername(), coins);
		} else {
			text = String.format("**%s** has **%s**.", user.getUsername(), coins);
		}
		BotUtils.sendMessage(Emoji.PURSE + " " + text, context.getChannel());
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show how many coins an user has.")
				.addArg("@user", "if not specified, it will show your coins", true)
				.build();
	}
}