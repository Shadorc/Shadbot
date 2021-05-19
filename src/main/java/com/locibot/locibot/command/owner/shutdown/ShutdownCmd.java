package com.locibot.locibot.command.owner.shutdown;

import com.locibot.locibot.LociBot;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.object.Emoji;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class ShutdownCmd extends BaseCmd {

    public ShutdownCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER,
                "shutdown", "Shutdown the bot");
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.createFollowupMessage(Emoji.QUESTION, "Do you really want to shutdown me? y/n")
                .doOnNext(__ -> ConfirmMessageInputs
                        .create(context.getClient(), Duration.ofSeconds(15), context.getChannelId(), LociBot.quit())
                        .listen());
    }

}
