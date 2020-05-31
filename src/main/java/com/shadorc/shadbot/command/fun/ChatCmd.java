package com.shadorc.shadbot.command.fun;

import com.shadorc.shadbot.api.json.pandorabots.ChatBotResponse;
import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NetUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import org.json.XML;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class ChatCmd extends BaseCmd {

    private static final String HOME_URl = "https://www.pandorabots.com/pandora/talk-xml";
    private static final Map<String, String> BOTS = Map.of("Marvin", "efc39100ce34d038",
            "Chomsky", "b0dafd24ee35a477",
            "R.I.V.K.A", "ea373c261e3458c6",
            "Lisa", "b0a6a41a5e345c23");
    private static final int MAX_CHARACTERS = 250;

    private final Map<Snowflake, String> channelsCustid;

    public ChatCmd() {
        super(CommandCategory.FUN, List.of("chat"));
        this.setDefaultRateLimiter();

        this.channelsCustid = new ConcurrentHashMap<>();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        if (arg.length() > MAX_CHARACTERS) {
            return Mono.error(new CommandException(String.format("The message must not exceed **%d characters**.",
                    MAX_CHARACTERS)));
        }

        return this.getResponse(context.getChannelId(), arg)
                .flatMap(response -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(Emoji.SPEECH + " " + response, channel)))
                .then();
    }

    private Mono<String> getResponse(Snowflake channelId, String input) {
        return Flux.fromIterable(BOTS.entrySet())
                .flatMap(bot -> Mono.defer(() -> this.talk(channelId, bot.getValue(), input)
                        .map(response -> String.format("**%s**: %s", bot.getKey(), response))))
                .takeUntil(str -> !str.isBlank())
                .next();
    }

    private Mono<String> talk(Snowflake channelId, String botId, String input) {
        final String url = String.format("%s?botid=%s&input=%s&custid=%s",
                HOME_URl, botId, NetUtils.encode(input), this.channelsCustid.getOrDefault(channelId, ""));

        return NetUtils.get(url)
                .map(XML::toJSONObject)
                .map(jsonObj -> jsonObj.getJSONObject("result"))
                .flatMap(resultObj -> Mono.fromCallable(() -> NetUtils.MAPPER.readValue(resultObj.toString(), ChatBotResponse.class)))
                .doOnNext(chat -> this.channelsCustid.put(channelId, chat.getCustId()))
                .map(ChatBotResponse::getResponse)
                .onErrorResume(err -> Mono.fromRunnable(() ->
                        DEFAULT_LOGGER.info("[{}] {} is not reachable, trying another one.",
                                this.getClass().getSimpleName(), botId)));
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Chat with an artificial intelligence.")
                .addArg("message", String.format("must not exceed %d characters", MAX_CHARACTERS), false)
                .setSource("https://www.pandorabots.com/"
                        + "\n**Marvin** (ID: efc39100ce34d038)"
                        + "\n**Chomsky** (ID: b0dafd24ee35a477)"
                        + "\n**R.I.V.K.A** (ID: ea373c261e3458c6)"
                        + "\n**Lisa** (ID: b0a6a41a5e345c23)")
                .build();
    }
}
