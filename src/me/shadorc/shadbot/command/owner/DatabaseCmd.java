package me.shadorc.shadbot.command.owner;

import java.util.List;

import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.data.db.DatabaseManager;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.publisher.Mono;

@Command(category = CommandCategory.OWNER, permission = CommandPermission.OWNER, names = { "database" })
public class DatabaseCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		List<String> args = context.requireArgs(2);

		final Long guildId = NumberUtils.asPositiveLong(args.get(0));
		if(guildId == null) {
			throw new CommandException(String.format("`%s` is not a valid guild ID.", args.get(0)));
		}

		return context.getClient().getGuildById(Snowflake.of(guildId))
				.switchIfEmpty(Mono.fromRunnable(() -> {
					throw new CommandException("Guild not found.");
				}))
				.map(guild -> {
					if(args.size() == 1) {
						return DatabaseManager.getDBGuild(guild.getId()).toString();
					}

					Long memberId = NumberUtils.asPositiveLong(args.get(1));
					if(memberId == null) {
						throw new CommandException(String.format("`%s` is not a valid member ID.", args.get(1)));
					}

					return DatabaseManager.getDBMember(guild.getId(), Snowflake.of(memberId)).toString();
				})
				.flatMap(text -> BotUtils.sendMessage(text, context.getChannel()))
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Return data about a member / guild.")
				.addArg("guildID", false)
				.addArg("memberID", true)
				.build();
	}

}
