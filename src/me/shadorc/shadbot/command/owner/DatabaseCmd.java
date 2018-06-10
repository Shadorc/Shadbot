package me.shadorc.shadbot.command.owner;

import java.util.List;

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

@Command(category = CommandCategory.OWNER, permission = CommandPermission.OWNER, names = { "database" })
public class DatabaseCmd extends AbstractCommand {

	@Override
	public void execute(Context context) {
		List<String> args = context.requireArgs(2);

		Long guildId = NumberUtils.asPositiveLong(args.get(0));
		if(guildId == null) {
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid guild ID.", args.get(0)));
		}

		context.getClient().getGuildById(Snowflake.of(guildId)).defaultIfEmpty(null).subscribe(guild -> {
			if(guild == null) {
				throw new IllegalCmdArgumentException("Guild not found.");
			}

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
	public EmbedCreateSpec getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Return raw database JSON about an user / a guild.")
				.addArg("guildID", false)
				.addArg("userID", true)
				.build();
	}

}
