package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.core.command.*;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.inputs.ConfirmMessageInputs;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class ShutdownCmd extends SubCmd {

    public ShutdownCmd(final GroupCmd groupCmd) {
        super(groupCmd, CommandCategory.OWNER, CommandPermission.OWNER, "shutdown", "Shutdown the bot");
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.createFollowupMessage(Emoji.QUESTION, "Do you really want to shutdown me? Type yes or no.")
                .doOnNext(__ -> new ConfirmMessageInputs(context.getClient(), Duration.ofSeconds(15),
                        context.getChannelId(), context.getAuthorId(), Shadbot.quit())
                        .listen());
    }

}
