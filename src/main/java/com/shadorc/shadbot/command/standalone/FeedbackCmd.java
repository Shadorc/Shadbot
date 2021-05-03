package com.shadorc.shadbot.command.standalone;

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
        super(CommandCategory.INFO, "feedback", "Send a feedback");
        this.setRateLimiter(new RateLimiter(1, Duration.ofMinutes(10)));
        this.addOption("message", "Your feedback", true, ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        final String message = context.getOptionAsString("message").orElseThrow();
        return context.getClient()
                .getUserById(Shadbot.getOwnerId())
                .flatMap(User::getPrivateChannel)
                .flatMap(channel -> DiscordUtil.sendMessage(Emoji.SPEECH, "Feedback from **%s** (User ID: %s, Guild ID: %s):%n%s"
                        .formatted(context.getAuthor().getTag(), context.getAuthorId().asString(),
                                context.getGuildId().asString(), message), channel))
                .then(context.createFollowupMessage(Emoji.INFO, context.localize("feedback.message")));
    }

}
