package me.shadorc.shadbot.command.currency;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.db.DBMember;
import me.shadorc.shadbot.data.db.DatabaseManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.CURRENCY, names = { "coins", "coin" })
public class CoinsCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		return context.getMessage()
				.getUserMentions()
				.switchIfEmpty(context.getAuthor())
				.next()
				.flatMap(user -> {
					final DBMember dbMember = DatabaseManager.getDBMember(context.getGuildId(), user.getId());
					final String coins = FormatUtils.formatCoins(dbMember.getCoins());

					String text;
					if(user.getId().equals(context.getAuthorId())) {
						text = String.format("(**%s**) You have **%s**.", user.getUsername(), coins);
					} else {
						text = String.format("**%s** has **%s**.", user.getUsername(), coins);
					}

					return BotUtils.sendMessage(Emoji.PURSE + " " + text, context.getChannel());
				})
				.then();

	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show how many coins an user has.")
				.addArg("@user", "if not specified, it will show your coins", true)
				.build();
	}
}