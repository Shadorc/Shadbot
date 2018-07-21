package me.shadorc.shadbot.command.game.trivia;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.api.trivia.category.TriviaCategoriesResponse;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.core.ratelimiter.RateLimiter;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited(cooldown = RateLimiter.GAME_COOLDOWN, max = 1)
@Command(category = CommandCategory.GAME, names = { "trivia" })
public class TriviaCmd extends AbstractCommand {

	protected static final ConcurrentHashMap<Snowflake, TriviaManager> MANAGERS = new ConcurrentHashMap<>();

	private TriviaCategoriesResponse categories;

	@Override
	public Mono<Void> execute(Context context) {
		if(categories == null) {
			try {
				final URL url = new URL("https://opentdb.com/api_category.php");
				this.categories = Utils.MAPPER.readValue(url, TriviaCategoriesResponse.class);
			} catch (IOException err) {
				throw Exceptions.propagate(err);
			}
		}

		if("categories".equalsIgnoreCase(context.getArg().orElse(null))) {
			return context.getAvatarUrl()
					.map(avatarUrl -> EmbedUtils.getDefaultEmbed()
							.setAuthor("Trivia categories", null, avatarUrl)
							.addField("ID", FormatUtils.format(categories.getIds(), id -> Integer.toString(id), "\n"), true)
							.addField("Name", String.join("\n", categories.getNames()), true))
					.flatMap(embed -> BotUtils.sendMessage(embed, context.getChannel()))
					.then();
		}

		Integer categoryId = NumberUtils.asPositiveInt(context.getArg().orElse(""));

		if(context.getArg().isPresent() && !categories.getIds().contains(categoryId)) {
			throw new CommandException(String.format("`%s` is not a valid ID. Use `%s%s categories` to see the complete list of categories.",
					context.getArg().get(), context.getPrefix(), this.getName()));
		}

		TriviaManager triviaManager = new TriviaManager(context, categoryId);
		if(MANAGERS.putIfAbsent(context.getChannelId(), triviaManager) == null) {
			return triviaManager.start();
		} else {
			return BotUtils.sendMessage(String.format(Emoji.INFO + " (**%s**) A Trivia game has already been started.",
					context.getUsername()), context.getChannel())
					.then();
		}
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Start a Trivia game in which everyone can participate.")
				.addArg("categoryID", "the category ID of the question", true)
				.addField("Category", String.format("Use `%s%s categories` to see the list of categories", context.getPrefix(), this.getName()), false)
				.setGains("The winner gets **%d coins** plus a bonus depending on his speed to answer.", TriviaManager.MIN_GAINS)
				.build();
	}
}
