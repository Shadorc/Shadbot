package com.shadorc.shadbot.command.fun;

import com.shadorc.shadbot.api.json.pandorabots.ChatBotResponse;
import com.shadorc.shadbot.api.json.pandorabots.ChatBotResult;
import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.NetUtils;
import discord4j.common.util.Snowflake;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
        super(CommandCategory.FUN, "chat", "Chat with an artificial intelligence");
        this.setDefaultRateLimiter();

        this.channelsCustid = new ConcurrentHashMap<>();
    }

    @Override
    public ApplicationCommandRequest build(ImmutableApplicationCommandRequest.Builder builder) {
        return builder
                .addOption(ApplicationCommandOptionData.builder()
                        .name("message")
                        .description(String.format("the message to send, must not exceed %d characters", MAX_CHARACTERS))
                        .type(ApplicationCommandOptionType.STRING.getValue())
                        .required(true)
                        .build())
                .build();
    }

    @Override
    public Mono<?> execute(Context context) {
        final String message = context.getOption("message").orElseThrow();
        if (message.length() > MAX_CHARACTERS) {
            return Mono.error(new CommandException(String.format("The message must not exceed **%d characters**.",
                    MAX_CHARACTERS)));
        }

        return this.getResponse(context.getChannelId(), message)
                .flatMap(response -> context.createFollowupMessage(Emoji.SPEECH + " " + response));
    }

    private Mono<String> getResponse(Snowflake channelId, String message) {
        return Flux.fromIterable(BOTS.entrySet())
                .flatMap(bot -> Mono.defer(() -> this.talk(channelId, bot.getValue(), message)
                        .map(response -> String.format("**%s**: %s", bot.getKey(), response))))
                .takeUntil(str -> !str.isBlank())
                .next();
    }

    private Mono<String> talk(Snowflake channelId, String botId, String message) {
        final String url = String.format("%s?botid=%s&input=%s&custid=%s",
                HOME_URl, botId, NetUtils.encode(message), this.channelsCustid.getOrDefault(channelId, ""));

        return RequestHelper.fromUrl(url)
                .to(ChatBotResponse.class)
                .map(ChatBotResponse::getResult)
                .doOnNext(chat -> this.channelsCustid.put(channelId, chat.getCustId()))
                .map(ChatBotResult::getResponse)
                .onErrorResume(err -> Mono.fromRunnable(() ->
                        DEFAULT_LOGGER.info("[{}] {} is not reachable, trying another one.",
                                this.getClass().getSimpleName(), botId)));
    }

}
