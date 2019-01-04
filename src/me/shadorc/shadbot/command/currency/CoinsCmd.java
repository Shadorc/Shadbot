package me.shadorc.shadbot.command.currency;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.database.DBMember;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
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
				.map(user -> {
					final DBMember dbMember = Shadbot.getDatabase().getDBMember(context.getGuildId(), user.getId());
					final String coins = FormatUtils.coins(dbMember.getCoins());
					if(user.getId().equals(context.getAuthorId())) {
						return String.format("(**%s**) You have **%s**.", user.getUsername(), coins);
					} else {
						return String.format("**%s** has **%s**.", user.getUsername(), coins);
					}
				})
				.flatMap(text -> context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(Emoji.PURSE + " " + text, channel)))
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