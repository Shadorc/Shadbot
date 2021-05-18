package com.shadorc.shadbot.command.setting;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.inputs.ConfirmMessageInputs;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class ResetSetting extends BaseCmd {

    protected ResetSetting() {
        super(CommandCategory.SETTING, CommandPermission.ADMIN, "reset", "Reset settings");
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.createFollowupMessage(Emoji.QUESTION, context.localize("reset.confirmation"))
                .doOnNext(__ -> new ConfirmMessageInputs(context.getClient(), Duration.ofSeconds(15),
                        context.getChannelId(), context.getAuthorId(), this.reset(context))
                        .listen());
    }

    private Mono<Message> reset(Context context) {
        return context.getDbGuild().resetSettings()
                .then(context.createFollowupMessage(Emoji.CHECK_MARK, context.localize("reset.message")));
    }
}
