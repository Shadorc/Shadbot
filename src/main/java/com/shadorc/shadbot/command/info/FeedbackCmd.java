package com.shadorc.shadbot.command.info;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.ratelimiter.RateLimiter;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

public class FeedbackCmd extends BaseCmd {

    public FeedbackCmd() {
        super(CommandCategory.INFO, List.of("feedback"));
        this.setRateLimiter(new RateLimiter(1, Duration.ofSeconds(5)));
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        return context.getClient()
                .getUserById(Shadbot.getOwnerId())
                .flatMap(User::getPrivateChannel)
                .cast(MessageChannel.class)
                .flatMap(channel -> DiscordUtils.sendMessage(
                        String.format(Emoji.SPEECH + " Feedback from **%s** (User ID: %d, Guild ID: %d):%n%s",
                                context.getUsername(), context.getAuthorId().asLong(), context.getGuildId().asLong(), arg), channel))
                .then(context.getChannel())
                .flatMap(channel -> DiscordUtils.sendMessage(
                        String.format(Emoji.INFO + " (**%s**) Feedback sent, thank you!", context.getUsername()), channel))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription(String.format("Send a message to my developer. This can be a bug report, a suggestion or " +
                        "anything related to Shadbot. You can also join the [support server](%s).", Config.SUPPORT_SERVER_URL))
                .addArg("text", false)
                .build();
    }
}
