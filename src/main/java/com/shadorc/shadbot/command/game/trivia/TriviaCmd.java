package com.shadorc.shadbot.command.game.trivia;

import com.shadorc.shadbot.api.trivia.category.TriviaCategoriesResponse;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.GameCmd;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.NetUtils;
import com.shadorc.shadbot.utils.NumberUtils;
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
        final Integer categoryId = NumberUtils.asPositiveInt(context.getArg().orElse(""));

        if (context.getArg().isPresent()) {
            if (this.categories == null) {
                this.categories = NetUtils.get(CATEGORY_URL, TriviaCategoriesResponse.class).block();
            }

            if ("categories".equalsIgnoreCase(context.getArg().get())) {
                final Consumer<EmbedCreateSpec> embedConsumer = DiscordUtils.getDefaultEmbed()
                        .andThen(embed -> embed.setAuthor("Trivia categories", null, context.getAvatarUrl())
                                .addField("ID", FormatUtils.format(this.categories.getIds(), Object::toString, "\n"), true)
                                .addField("Name", String.join("\n", this.categories.getNames()), true));

                return context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(embedConsumer, channel))
                        .then();
            }

            if (!this.categories.getIds().contains(categoryId)) {
                return Mono.error(new CommandException(String.format("`%s` is not a valid ID. Use `%s%s categories` to see the complete list of categories.",
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
            triviaManager.start();
            return triviaManager.show();
        }

    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Start a Trivia game in which everyone can participate.")
                .addArg("categoryID", "the category ID of the question", true)
                .addField("Category", String.format("Use `%s%s categories` to see the list of categories",
                        context.getPrefix(), this.getName()), false)
                .addField("Gains", String.format("The winner gets **%d coins** plus a bonus (**%d coins max.**) depending on his speed to answer.",
                        TriviaGame.MIN_GAINS, TriviaGame.MAX_BONUS), false)
                .build();
    }
}
