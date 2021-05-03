package com.shadorc.shadbot.command.owner.shutdown;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class ShutdownCmd extends BaseCmd {

    public ShutdownCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, "shutdown", "Shutdown the bot");
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.createFollowupMessage(Emoji.QUESTION, "Do you really want to shutdown me? y/n")
                .doOnNext(__ -> ConfirmMessageInputs
                        .create(context.getClient(), Duration.ofSeconds(15), context.getChannelId(), Shadbot.quit())
                        .listen());
    }

}
