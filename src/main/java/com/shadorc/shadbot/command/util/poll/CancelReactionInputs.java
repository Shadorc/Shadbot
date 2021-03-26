package com.shadorc.shadbot.command.util.poll;

import com.shadorc.shadbot.object.inputs.ReactionInputs;
import com.shadorc.shadbot.utils.DiscordUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.rest.util.Permission;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

public class CancelReactionInputs extends ReactionInputs {

    private final PollManager manager;
    private final AtomicBoolean isCanceled;

    private CancelReactionInputs(PollManager manager, Snowflake messageId, ReactionEmoji reactionEmoji) {
        super(manager.getContext().getClient(), messageId, reactionEmoji, manager.getSpec().getDuration());
        this.manager = manager;
        this.isCanceled = new AtomicBoolean(false);
    }

    public static CancelReactionInputs create(PollManager manager, Snowflake messageId, ReactionEmoji reactionEmoji) {
        return new CancelReactionInputs(manager, messageId, reactionEmoji);
    }

    public Mono<Void> addReaction() {
        return this.gateway.getMessageById(this.manager.getContext().getChannelId(), this.messageId)
                .flatMap(message -> message.addReaction(this.reactionEmoji));
    }

    @Override
    public boolean takeEventWile(ReactionAddEvent event) {
        return !this.isCanceled.get();
    }

    @Override
    public Mono<?> onReactionAddEvent(ReactionAddEvent event) {
        final boolean isAuthor = event.getUserId().equals(this.manager.getContext().getAuthorId());
        final Mono<Boolean> isAdmin = event.getChannel()
                .flatMap(channel -> DiscordUtil.hasPermission(channel, event.getUserId(), Permission.ADMINISTRATOR));

        return BooleanUtils.or(Mono.just(isAuthor), isAdmin)
                .filter(Boolean.TRUE::equals)
                .flatMap(__ -> {
                    this.isCanceled.set(true);
                    return this.manager.getPollCmd().cancel(this.manager.getContext());
                });
    }
}
