package com.shadorc.shadbot.command.game.trivia;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.game.GameCmd;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

public class TriviaCmd extends GameCmd<TriviaGame> {

    // https://opentdb.com/api_category.php <ID, Name>
    private static final Map<String, Integer> CATEGORIES = new HashMap<>();

    static {
        CATEGORIES.put("General Knowledge", 9);
        CATEGORIES.put("Entertainment: Books", 10);
        CATEGORIES.put("Entertainment: Film", 11);
        CATEGORIES.put("Entertainment: Music", 12);
        CATEGORIES.put("Entertainment: Musicals & Theatres", 13);
        CATEGORIES.put("Entertainment: Television", 14);
        CATEGORIES.put("Entertainment: Video Games", 15);
        CATEGORIES.put("Entertainment: Board Games", 16);
        CATEGORIES.put("Science & Nature", 17);
        CATEGORIES.put("Science: Computers", 18);
        CATEGORIES.put("Science: Mathematics", 19);
        CATEGORIES.put("Mythology", 20);
        CATEGORIES.put("Sports", 21);
        CATEGORIES.put("Geography", 22);
        CATEGORIES.put("History", 23);
        CATEGORIES.put("Politics", 24);
        CATEGORIES.put("Art", 25);
        CATEGORIES.put("Celebrities", 26);
        CATEGORIES.put("Animals", 27);
        CATEGORIES.put("Vehicles", 28);
        CATEGORIES.put("Entertainment: Comics", 29);
        CATEGORIES.put("Science: Gadgets", 30);
        CATEGORIES.put("Entertainment: Japanese Anime & Manga", 31);
        CATEGORIES.put("Entertainment: Cartoon & Animations", 32);
    }

    public TriviaCmd() {
        super("trivia", "Start a Trivia game in which everyone can participate.");
        this.addOption(option -> option.name("category")
                .description("The category of the question")
                .required(false)
                .type(ApplicationCommandOptionType.STRING.getValue())
                .choices(DiscordUtil.toOptions(CATEGORIES.keySet())));
    }

    @Override
    public Mono<?> execute(Context context) {
        final Integer categoryId = context.getOptionAsString("category")
                .map(CATEGORIES::get)
                .orElse(null);

        if (this.isGameStarted(context.getChannelId())) {
            return context.reply(Emoji.INFO, context.localize("trivia.already.started"));
        } else {
            final TriviaGame game = new TriviaGame(context, categoryId);
            this.addGame(context.getChannelId(), game);
            return game.start()
                    .then(game.show())
                    .doOnError(err -> game.destroy());
        }
    }

}
