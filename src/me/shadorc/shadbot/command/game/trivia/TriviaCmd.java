package me.shadorc.shadbot.command.game.trivia;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.api.trivia.category.TriviaCategoriesResponse;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

public class TriviaCmd extends BaseCmd {

	private static final String CATEGORY_URL = "https://opentdb.com/api_category.php";

	protected static final ConcurrentHashMap<Snowflake, TriviaManager> MANAGERS = new ConcurrentHashMap<>();

	private TriviaCategoriesResponse categories;

	public TriviaCmd() {
		super(CommandCategory.GAME, List.of("trivia"));
		this.setGameRateLimiter();

		this.categories = null;
	}

	@Override
	public Mono<Void> execute(Context context) {
		if(this.categories == null) {
			try {
				this.categories = Utils.MAPPER.readValue(NetUtils.getJSON(CATEGORY_URL), TriviaCategoriesResponse.class);
			} catch (final IOException err) {
				throw Exceptions.propagate(err);
			}
		}

		if("categories".equalsIgnoreCase(context.getArg().orElse(null))) {
			final Consumer<EmbedCreateSpec> embedConsumer = EmbedUtils.getDefaultEmbed()
					.andThen(embed -> embed.setAuthor("Trivia categories", null, context.getAvatarUrl())
							.addField("ID", FormatUtils.format(this.categories.getIds(), id -> Integer.toString(id), "\n"), true)
							.addField("Name", String.join("\n", this.categories.getNames()), true));

			return context.getChannel()
					.flatMap(channel -> DiscordUtils.sendMessage(embedConsumer, channel))
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
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Start a Trivia game in which everyone can participate.")
				.addArg("categoryID", "the category ID of the question", true)
				.addField("Category", String.format("Use `%s%s categories` to see the list of categories", context.getPrefix(), this.getName()), false)
				.setGains("The winner gets **%d coins** plus a bonus depending on his speed to answer.", TriviaManager.MIN_GAINS)
				.build();
	}
}
