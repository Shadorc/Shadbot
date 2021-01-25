package com.shadorc.shadbot.command.info;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.ratelimiter.RateLimiter;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import discord4j.core.object.entity.User;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class FeedbackCmd extends BaseCmd {

    public FeedbackCmd() {
        super(CommandCategory.INFO, "feedback",
                "Send a message to my developer. This can be anything related to the bot");
        this.setRateLimiter(new RateLimiter(1, Duration.ofSeconds(5)));
        this.addOption("message", "The message to send", true, ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.getClient()
                .getUserById(Shadbot.getOwnerId())
                .flatMap(User::getPrivateChannel)
                .flatMap(channel -> DiscordUtil.sendMessage(
                        String.format(Emoji.SPEECH + " Feedback from **%s** (User ID: %d, Guild ID: %d):%n%s",
                                context.getAuthorName(), context.getAuthorId().asLong(),
                                context.getGuildId().asLong(), context.getOption("message").orElseThrow()), channel))
                .then(context.createFollowupMessage(Emoji.INFO + " (**%s**) Feedback sent, thank you!", context.getAuthorName()));
    }

}
