package me.shadorc.shadbot.command.admin;

import java.util.List;

import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@Command(category = CommandCategory.ADMIN, permission = CommandPermission.ADMIN, names = { "manage_coins", "manage-coins", "managecoins" })
public class ManageCoinsCmd extends AbstractCommand {

	private enum Action {
		ADD, REMOVE, RESET;
	}

	@Override
	public void execute(Context context) {
		List<String> args = context.requireArgs(2, 3);

		Action action = Utils.getValueOrNull(Action.class, args.get(0));
		if(action == null) {
			throw new CommandException(String.format("`%s` is not a valid action. %s",
					args.get(0), FormatUtils.formatOptions(Action.class)));
		}

		Integer coins = NumberUtils.asInt(args.get(1));
		if(coins == null && !Action.RESET.equals(action)) {
			throw new CommandException(String.format("`%s` is not a valid amount for coins.", args.get(1)));
		}

		if(context.getMessage().getUserMentionIds().isEmpty() && context.getMessage().getRoleMentionIds().isEmpty()) {
			throw new CommandException("You must specify at least one user / role.");
		}

		BotUtils.getUsersFrom(context.getMessage()).buffer().subscribe(users -> {
			String mentionsStr = context.getMessage().mentionsEveryone() ? "Everyone" : FormatUtils.format(users, User::getUsername, ", ");
			switch (action) {
				case ADD:
					users.stream().forEach(user -> Database.getDBMember(context.getGuildId().get(), user.getId()).addCoins(coins));
					BotUtils.sendMessage(String.format(Emoji.MONEY_BAG + " **%s** received **%s**.",
							mentionsStr, FormatUtils.formatCoins(coins)), context.getChannel());
					break;
				case REMOVE:
					users.stream().forEach(user -> Database.getDBMember(context.getGuildId().get(), user.getId()).addCoins(-coins));
					BotUtils.sendMessage(String.format(Emoji.MONEY_BAG + " **%s** lost **%s**.",
							mentionsStr, FormatUtils.formatCoins(coins)), context.getChannel());
					break;
				case RESET:
					users.stream().forEach(user -> Database.getDBMember(context.getGuildId().get(), user.getId()).resetCoins());
					BotUtils.sendMessage(String.format(Emoji.MONEY_BAG + " **%s** lost all %s coins.",
							mentionsStr, users.size() == 1 ? "his" : "their"), context.getChannel());
					break;
			}
		});
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Manage user(s) coins.")
				.addArg("action", FormatUtils.format(Action.values(), action -> action.toString().toLowerCase(), " / "), false)
				.addArg("coins", "can be positive or negative", true)
				.addArg("@user(s)/@role(s)", false)
				.setExample(String.format("`%s%s add 150 @Shadbot`%n`%s%s reset @Shadbot`",
						context.getPrefix(), this.getName(), context.getPrefix(), this.getName()))
				.build();
	}
}
