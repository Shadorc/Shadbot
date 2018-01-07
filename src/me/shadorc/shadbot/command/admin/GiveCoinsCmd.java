package me.shadorc.shadbot.command.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.CastUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;

@Command(category = CommandCategory.ADMIN, permission = CommandPermission.ADMIN, names = { "give_coins", "add_coins" })
public class GiveCoinsCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		List<String> splitArgs = StringUtils.split(context.getArg());
		if(splitArgs.size() != 2) {
			throw new MissingArgumentException();
		}

		Integer coins = CastUtils.asInt(splitArgs.get(0));
		if(coins == null) {
			throw new IllegalCmdArgumentException("Invalid amount.");
		}

		List<IUser> users = new ArrayList<>();
		users.addAll(context.getMessage().getMentions());
		for(IRole role : context.getMessage().getRoleMentions()) {
			users.addAll(context.getGuild().getUsersByRole(role));
		}
		users = users.stream().distinct().collect(Collectors.toList());

		users.stream().forEach(user -> Database.getDBUser(context.getGuild(), user).addCoins(coins));

		String msg;
		if(context.getMessage().mentionsEveryone()) {
			msg = "Everyone";
		} else {
			msg = FormatUtils.format(users, IUser::getName, ", ");
		}

		BotUtils.sendMessage(String.format(Emoji.MONEY_BAG + " **%s** received **%s**.",
				msg, FormatUtils.formatCoins(coins)), context.getChannel());
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Give coins to user(s) and/or role(s).")
				.addArg("coins", "can be positive or negative", false)
				.addArg("@user(s)/@role(s)", false)
				.build();
	}
}
