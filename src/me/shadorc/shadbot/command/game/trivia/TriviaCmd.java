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
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited(cooldown = RateLimiter.GAME_COOLDOWN, max = 1)
@Command(category = CommandCategory.GAME, names = { "trivia" })
public class TriviaCmd extends AbstractCommand {

	protected static final ConcurrentHashMap<Snowflake, TriviaManager> MANAGERS = new ConcurrentHashMap<>();

	private TriviaCategoriesResponse categories;

	@Override
	public Mono<Void> execute(Context context) {
		if(this.categories == null) {
			try {
				final URL url = new URL("https://opentdb.com/api_category.php");
				this.categories = Utils.MAPPER.readValue(url, TriviaCategoriesResponse.class);
			} catch (final IOException err) {
				throw Exceptions.propagate(err);
			}
		}

		if("categories".equalsIgnoreCase(context.getArg().orElse(null))) {
			return context.getAvatarUrl()
					.map(avatarUrl -> EmbedUtils.getDefaultEmbed()
							.setAuthor("Trivia categories", null, avatarUrl)
							.addField("ID", FormatUtils.format(this.categories.getIds(), id -> Integer.toString(id), "\n"), true)
							.addField("Name", String.join("\n", this.categories.getNames()), true))
					.flatMap(embed -> context.getChannel()
							.flatMap(channel -> DiscordUtils.sendMessage(embed, channel)))
					.then();
		}

		final Integer categoryId = NumberUtils.asPositiveInt(context.getArg().orElse(""));

		if(context.getArg().isPresent() && !this.categories.getIds().contains(categoryId)) {
			throw new CommandException(String.format("`%s` is not a valid ID. Use `%s%s categories` to see the complete list of categories.",
					context.getArg().get(), context.getPrefix(), this.getName()));
		}

		final TriviaManager triviaManager = new TriviaManager(context, categoryId);
		if(MANAGERS.putIfAbsent(context.getChannelId(), triviaManager) == null) {
			triviaManager.start();
			return triviaManager.show();
		} else {
			return context.getChannel()
					.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.INFO + " (**%s**) A Trivia game has already been started.",
							context.getUsername()), channel))
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
