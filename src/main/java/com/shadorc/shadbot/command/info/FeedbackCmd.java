package com.shadorc.shadbot.command.info;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.ratelimiter.RateLimiter;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class FeedbackCmd extends BaseCmd {

    public FeedbackCmd() {
        super(CommandCategory.INFO, "feedback",
                "Send a message to my developer. This can be a bug report, a suggestion or anything related to the bot.");
        this.setRateLimiter(new RateLimiter(1, Duration.ofSeconds(5)));
    }

    @Override
    public ApplicationCommandRequest build(ImmutableApplicationCommandRequest.Builder builder) {
        return builder.addOption(ApplicationCommandOptionData.builder()
                .name("text")
                .description("The message to send")
                .type(ApplicationCommandOptionType.STRING.getValue())
                .required(true)
                .build())
                .build();
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.getClient()
                .getUserById(Shadbot.getOwnerId())
                .flatMap(User::getPrivateChannel)
                .flatMap(channel -> DiscordUtils.sendMessage(
                        String.format(Emoji.SPEECH + " Feedback from **%s** (User ID: %d, Guild ID: %d):%n%s",
                                context.getAuthorName(), context.getAuthorId().asLong(),
                                context.getGuildId().asLong(), context.getOption("text").orElseThrow()), channel))
                .then(context.createFollowupMessage(Emoji.INFO + " (**%s**) Feedback sent, thank you!", context.getAuthorName()));
    }

}
