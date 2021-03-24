package com.shadorc.shadbot.command.fun;

import com.shadorc.shadbot.api.json.pandorabots.ChatBotResponse;
import com.shadorc.shadbot.api.json.pandorabots.ChatBotResult;
import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.LogUtil;
import com.shadorc.shadbot.utils.NetUtil;
import discord4j.common.util.Snowflake;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class ChatCmd extends BaseCmd {

    private static final Logger LOGGER = LogUtil.getLogger(ChatCmd.class, LogUtil.Category.COMMAND);
    private static final String HOME_URl = "https://www.pandorabots.com/pandora/talk-xml";
    private static final int MAX_CHARACTERS = 250;
    private static final Map<String, String> BOTS = new LinkedHashMap<>(4);

    static {
        BOTS.put("Marvin", "efc39100ce34d038");
        BOTS.put("Chomsky", "b0dafd24ee35a477");
        BOTS.put("R.I.V.K.A", "ea373c261e3458c6");
        BOTS.put("Lisa", "b0a6a41a5e345c23");
    }

    private final Map<Snowflake, String> channelsCustid;

    public ChatCmd() {
        super(CommandCategory.FUN, "chat", "Chat with an artificial intelligence");
        this.addOption("message", "The message to send, must not exceed %d characters".formatted(MAX_CHARACTERS),
                true, ApplicationCommandOptionType.STRING);

        this.channelsCustid = new ConcurrentHashMap<>();
    }

    @Override
    public Mono<?> execute(Context context) {
        final String message = context.getOptionAsString("message").orElseThrow();
        if (message.length() > MAX_CHARACTERS) {
            return Mono.error(new CommandException(context.localize("chat.max.characters")
                    .formatted(MAX_CHARACTERS)));
        }

        return this.getResponse(context.getChannelId(), message)
                .flatMap(response -> context.reply(Emoji.SPEECH, response));
    }

    private Mono<String> getResponse(Snowflake channelId, String message) {
        return Flux.fromIterable(BOTS.entrySet())
                .flatMapSequential(bot -> this.talk(channelId, bot.getValue(), message)
                        .map(response -> "**%s**: %s".formatted(bot.getKey(), response)))
                .takeUntil(Predicate.not(String::isBlank))
                .next();
    }

    private Mono<String> talk(Snowflake channelId, String botId, String message) {
        final String url = "%s?".formatted(HOME_URl)
                + "botid=%s".formatted(botId)
                + "&input=%s".formatted(NetUtil.encode(message))
                + "&custid=%s".formatted(this.channelsCustid.getOrDefault(channelId, ""));

        return RequestHelper.fromUrl(url)
                .to(ChatBotResponse.class)
                .map(ChatBotResponse::getResult)
                .doOnNext(chat -> this.channelsCustid.put(channelId, chat.getCustId()))
                .map(ChatBotResult::getResponse)
                .onErrorResume(err -> Mono.fromRunnable(() ->
                        LOGGER.info("{} is not reachable, trying another one.", botId)));
    }

}
