package me.shadorc.shadbot.command.currency;

import discord4j.core.object.entity.Guild;
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
import reactor.util.function.Tuples;

@RateLimited
@Command(category = CommandCategory.CURRENCY, names = { "coins", "coin" })
public class CoinsCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		context.getMessage().getUserMentions()
				.switchIfEmpty(context.getAuthor())
				.single()
				.flatMap(user -> context.getGuild()
						.map(guild -> Tuples.of(guild, user)))
				.subscribe(guildAndUser -> this.execute(context, guildAndUser.getT1(), guildAndUser.getT2()));

	}

	private void execute(Context context, Guild guild, User user) {
		DBMember dbMember = Database.getDBMember(guild.getId(), user.getId());
		String coins = FormatUtils.formatCoins(dbMember.getCoins());
		String text;
		if(user.getId().equals(context.getAuthorId())) {
			text = String.format("(**%s**) You have **%s**.", context.getUsername(), coins);
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