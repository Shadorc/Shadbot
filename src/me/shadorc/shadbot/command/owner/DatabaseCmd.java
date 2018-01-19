package me.shadorc.shadbot.command.owner;

import java.util.List;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.data.db.DBGuild;
import me.shadorc.shadbot.data.db.DBUser;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.CastUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IGuild;

@Command(category = CommandCategory.OWNER, permission = CommandPermission.OWNER, names = { "database" })
public class DatabaseCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		List<String> splitArgs = StringUtils.split(context.getArg());
		if(splitArgs.size() > 2) {
			throw new MissingArgumentException();
		}

		Long guildID = CastUtils.asPositiveLong(splitArgs.get(0));
		if(guildID == null) {
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid guild ID.", splitArgs.get(0)));
		}

		IGuild guild = context.getClient().getGuildByID(guildID);
		if(guild == null) {
			throw new IllegalCmdArgumentException("Guild not found.");
		}

		String json = null;
		if(splitArgs.size() == 1) {
			DBGuild dbGuild = new DBGuild(guild);
			json = dbGuild.toJSON().toString(Config.JSON_INDENT_FACTOR);

		} else if(splitArgs.size() == 2) {
			Long userID = CastUtils.asPositiveLong(splitArgs.get(1));
			if(userID == null) {
				throw new IllegalCmdArgumentException(String.format("`%s` is not a valid user ID.", splitArgs.get(0)));
			}

			DBUser dbUser = new DBUser(guild, userID);
			json = dbUser.toJSON().toString(Config.JSON_INDENT_FACTOR);
		}

		if(json == null || json.length() == 2) {
			BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " Nothing found.", context.getChannel());
		} else {
			BotUtils.sendMessage(json, context.getChannel());
		}
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Return raw database JSON about an user / a guild.")
				.addArg("guildID", false)
				.addArg("userID", true)
				.build();
	}

}
