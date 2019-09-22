package com.shadorc.shadbot.command.french;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.NetUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.spec.EmbedCreateSpec;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class JokeCmd extends BaseCmd {

    private static final String HOME_URL = "https://www.humour.com/blagues/";

    public JokeCmd() {
        super(CommandCategory.FRENCH, List.of("blague", "joke"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());
        return NetUtils.get(HOME_URL)
                .map(Jsoup::parse)
                // Get all elements representing a joke
                .map(doc -> doc.getElementsByClass("gag__content"))
                .flatMapMany(Flux::fromIterable)
                .map(Element::html)
                // Filter joke with more than 1000 characters
                .filter(element -> element.length() < 1000)
                .collectList()
                // Pick a random joke
                .map(Utils::randValue)
                .map(joke -> joke.split("<br>"))
                // Remove HTML codes in joke string
                .map(lines -> FormatUtils.format(lines, line -> Jsoup.parse(line).text().trim(), "\n"))
                .map(joke -> updatableMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                        .andThen(embed -> embed.setAuthor("Blague", HOME_URL, context.getAvatarUrl())
                                .setDescription(joke))))
                .flatMap(UpdatableMessage::send)
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show a random French joke.")
                .setSource(HOME_URL)
                .build();
    }
}
