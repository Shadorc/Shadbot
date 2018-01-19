package me.shadorc.shadbot.command.admin;

import java.util.List;

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
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IUser;

@Command(category = CommandCategory.ADMIN, permission = CommandPermission.ADMIN, names = { "manage_coins", "manage-coins", "managecoins" })
public class ManageCoinsCmd extends AbstractCommand {

	private enum Action {
		ADD, REMOVE, RESET;
	}

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		List<String> splitArgs = StringUtils.split(context.getArg());
		if(!Utils.isInRange(splitArgs.size(), 2, 3)) {
			throw new MissingArgumentException();
		}

		Action action = Utils.getValueOrNull(Action.class, splitArgs.get(0));
		if(action == null) {
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid action. %s",
					splitArgs.get(0), FormatUtils.formatOptions(Action.class)));
		}

		Integer coins = CastUtils.asInt(splitArgs.get(1));
		if(coins == null && action != Action.RESET) {
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid amount for coins.", splitArgs.get(1)));
		}

		List<IUser> users = BotUtils.getUsersFrom(context.getMessage());
		if(users.isEmpty()) {
			throw new IllegalCmdArgumentException("You must specify at least one user / role.");
		}

		String mentionsStr = context.getMessage().mentionsEveryone() ? "Everyone" : FormatUtils.format(users, IUser::getName, ", ");
		switch (action) {
			case ADD:
				users.stream().forEach(user -> Database.getDBUser(context.getGuild(), user).addCoins(coins));
				BotUtils.sendMessage(String.format(Emoji.MONEY_BAG + " **%s** received **%s**.",
						mentionsStr, FormatUtils.formatCoins(coins)), context.getChannel());
				break;
			case REMOVE:
				users.stream().forEach(user -> Database.getDBUser(context.getGuild(), user).addCoins(-coins));
				BotUtils.sendMessage(String.format(Emoji.MONEY_BAG + " **%s** lost **%s**.",
						mentionsStr, FormatUtils.formatCoins(coins)), context.getChannel());
				break;
			case RESET:
				users.stream().forEach(user -> Database.getDBUser(context.getGuild(), user).resetCoins());
				BotUtils.sendMessage(String.format(Emoji.MONEY_BAG + " **%s** lost all %s coins.",
						mentionsStr, users.size() == 1 ? "his" : "their"), context.getChannel());
				break;
		}
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Manage user(s) coins.")
				.addArg("action", FormatUtils.format(Action.values(), action -> action.toString().toLowerCase(), " / "), false)
				.addArg("coins", "can be positive or negative", true)
				.addArg("@user(s)/@role(s)", false)
				.setExample(String.format("`%s%s add 150 @Shadbot`%n`%s%s reset @Shadbot`", prefix, this.getName(), prefix, this.getName()))
				.build();
	}
}
