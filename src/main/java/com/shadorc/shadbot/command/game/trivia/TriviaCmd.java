package com.shadorc.shadbot.command.game.trivia;

import com.shadorc.shadbot.api.json.trivia.category.TriviaCategoriesResponse;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.GameCmd;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.*;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class TriviaCmd extends GameCmd<TriviaGame> {

    private static final String CATEGORY_URL = "https://opentdb.com/api_category.php";

    private TriviaCategoriesResponse categories;

    public TriviaCmd() {
        super(List.of("trivia"));

        this.categories = null;
    }

    @Override
    public Mono<Void> execute(Context context) {
        return this.getCategories()
                .then(Mono.defer(() -> {
                    final Integer categoryId = NumberUtils.toPositiveIntOrNull(context.getArg().orElse(""));

                    if (context.getArg().isPresent()) {
                        // Display a list of available categories
                        if ("categories".equalsIgnoreCase(context.getArg().get())) {
                            final String ids = FormatUtils.format(this.categories.getIds(), Object::toString, "\n");
                            final String names = String.join("\n", this.categories.getNames());
                            final Consumer<EmbedCreateSpec> embedConsumer = DiscordUtils.getDefaultEmbed()
                                    .andThen(embed -> embed.setAuthor("Trivia categories", null, context.getAvatarUrl())
                                            .addField("ID", ids, true)
                                            .addField("Name", names, true));

                            return context.getChannel()
                                    .flatMap(channel -> DiscordUtils.sendMessage(embedConsumer, channel))
                                    .then();
                        }

                        // The user tries to access a category that does not exist
                        else if (!this.categories.getIds().contains(categoryId)) {
                            return Mono.error(new CommandException(
                                    String.format("`%s` is not a valid ID. Use `%s%s categories` to see the "
                                                    + "complete list of available categories.",
                                            context.getArg().get(), context.getPrefix(), this.getName())));
                        }
                    }

                    if (this.getManagers().containsKey(context.getChannelId())) {
                        return context.getChannel()
                                .flatMap(channel -> DiscordUtils.sendMessage(String.format(
                                        Emoji.INFO + " (**%s**) A Trivia game has already been started.",
                                        context.getUsername()), channel))
                                .then();
                    } else {
                        final TriviaGame triviaManager = new TriviaGame(this, context, categoryId);
                        this.getManagers().put(context.getChannelId(), triviaManager);
                        return triviaManager.start()
                                .then(triviaManager.show())
                                .doOnError(err -> this.getManagers().remove(context.getChannelId()));
                    }
                }));
    }

    private Mono<TriviaCategoriesResponse> getCategories() {
        final Mono<TriviaCategoriesResponse> getCategories = NetUtils.get(CATEGORY_URL, TriviaCategoriesResponse.class)
                .doOnNext(categories -> {
                    this.categories = categories;
                    LogUtils.info("Open Trivia DB categories obtained.");
                });

        return Mono.justOrEmpty(this.categories)
                .switchIfEmpty(getCategories);
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Start a Trivia game in which everyone can participate.")
                .addArg("categoryID", "the category ID of the question", true)
                .addField("Category", String.format("Use `%s%s categories` to see the list of categories",
                        context.getPrefix(), this.getName()), false)
                .addField("Gains", String.format("The winner gets **%s** plus a bonus (**%s max.**) depending " +
                                "on his speed to answer.",
                        FormatUtils.coins(TriviaGame.MIN_GAINS), FormatUtils.coins(TriviaGame.MAX_BONUS)), false)
                .build();
    }
}
