package me.shadorc.shadbot.command.owner;

import java.util.List;
import java.util.Optional;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.data.db.DBGuild;
import me.shadorc.shadbot.data.db.DBMember;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@Command(category = CommandCategory.OWNER, permission = CommandPermission.OWNER, names = { "database" })
public class DatabaseCmd extends AbstractCommand {

	@Override
	public void execute(Context context) {
		List<String> args = context.requireArgs(2);

		final Long guildId = NumberUtils.asPositiveLong(args.get(0));
		if(guildId == null) {
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid guild ID.", args.get(0)));
		}

		context.getClient().getGuildById(Snowflake.of(guildId))
				.map(Optional::of)
				.defaultIfEmpty(Optional.empty())
				.subscribe(optGuild -> {
					if(!optGuild.isPresent()) {
						throw new IllegalCmdArgumentException("Guild not found.");
					}

					final Guild guild = optGuild.get();

					String json = null;
					if(args.size() == 1) {
						DBGuild dbGuild = Database.getDBGuild(guild.getId());
						json = dbGuild.toJSON().toString(Config.JSON_INDENT_FACTOR);

					} else if(args.size() == 2) {
						Long userId = NumberUtils.asPositiveLong(args.get(1));
						if(userId == null) {
							throw new IllegalCmdArgumentException(String.format("`%s` is not a valid user ID.", args.get(0)));
						}

						DBMember dbUser = new DBMember(guild.getId(), Snowflake.of(userId));
						json = dbUser.toJSON().toString(Config.JSON_INDENT_FACTOR);
					}

					if(json == null || json.length() == 2) {
						BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " Nothing found.", context.getChannel());
					} else {
						BotUtils.sendMessage(json, context.getChannel());
					}
				});
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Return raw database JSON about an user / a guild.")
				.addArg("guildID", false)
				.addArg("userID", true)
				.build();
	}

}
