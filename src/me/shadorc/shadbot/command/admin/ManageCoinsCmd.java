package me.shadorc.shadbot.command.admin;

import java.util.List;
import java.util.function.Consumer;

import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@Command(category = CommandCategory.ADMIN, permission = CommandPermission.ADMIN, names = { "manage_coins", "manage-coins", "managecoins" })
public class ManageCoinsCmd extends AbstractCommand {

	private enum Action {
		ADD, REMOVE, RESET;
	}

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(2, 3);

		final Action action = Utils.getEnum(Action.class, args.get(0));
		if(action == null) {
			throw new CommandException(String.format("`%s` is not a valid action. %s",
					args.get(0), FormatUtils.options(Action.class)));
		}

		final Integer coins = NumberUtils.asInt(args.get(1));
		if(coins == null && !Action.RESET.equals(action)) {
			throw new CommandException(String.format("`%s` is not a valid amount for coins.", args.get(1)));
		}

		return DiscordUtils.getMembersFrom(context.getMessage())
				.collectList()
				.map(members -> {
					if(members.isEmpty()) {
						throw new CommandException("You must specify at least one user / role.");
					}

					final String mentionsStr = context.getMessage().mentionsEveryone() ? "Everyone" : FormatUtils.format(members, User::getUsername, ", ");
					switch (action) {
						case ADD:
							members.stream().forEach(user -> Shadbot.getDatabase().getDBMember(context.getGuildId(), user.getId()).addCoins(coins));
							return String.format(Emoji.MONEY_BAG + " **%s** received **%s**.", mentionsStr, FormatUtils.coins(coins));
						case REMOVE:
							members.stream().forEach(user -> Shadbot.getDatabase().getDBMember(context.getGuildId(), user.getId()).addCoins(-coins));
							return String.format(Emoji.MONEY_BAG + " **%s** lost **%s**.", mentionsStr, FormatUtils.coins(coins));
						case RESET:
							members.stream().forEach(user -> Shadbot.getDatabase().getDBMember(context.getGuildId(), user.getId()).resetCoins());
							return String.format(Emoji.MONEY_BAG + " **%s** lost all %s coins.", mentionsStr, members.size() == 1 ? "his" : "their");
						default:
							return null;
					}
				})
				.flatMap(text -> context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(text, channel)))
				.then();
	}

	@Override
	public Mono<Consumer<? super EmbedCreateSpec>> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Manage user(s) coins.")
				.addArg("action", FormatUtils.format(Action.class, " / "), false)
				.addArg("coins", "can be positive or negative", true)
				.addArg("@user(s)/@role(s)", false)
				.setExample(String.format("`%s%s add 150 @Shadbot`%n`%s%s reset @Shadbot`",
						context.getPrefix(), this.getName(), context.getPrefix(), this.getName()))
				.build();
	}
}
