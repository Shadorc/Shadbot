package com.shadorc.shadbot.command.game.dice;

import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.Inputs;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import reactor.core.publisher.Mono;

public class DiceInputs extends Inputs {

    private final DiceGame game;

    public DiceInputs(GatewayDiscordClient client, DiceGame game) {
        super(client, game.getDuration());
        this.game = game;
    }

    @Override
    public Mono<Boolean> isValidEvent(MessageCreateEvent event) {
        if (event.getMessage().getContent().isEmpty() || event.getMember().isEmpty()) {
            return Mono.just(false);
        }

        if (!event.getMessage().getChannelId().equals(this.game.getContext().getChannelId())) {
            return Mono.just(false);
        }

        final Member member = event.getMember().get();
        return this.game.isCancelMessage(event.getMessage())
                .map(isCancelCmd -> isCancelCmd || this.game.getPlayers().containsKey(member.getId()));
    }

    @Override
    public boolean takeEventWile(MessageCreateEvent event) {
        return this.game.isScheduled();
    }

    @Override
    public Mono<Void> processEvent(MessageCreateEvent event) {
        return Mono.justOrEmpty(event.getMember())
                .filterWhen(ignored -> this.game.isCancelMessage(event.getMessage()))
                .flatMap(member -> event.getMessage().getChannel()
                        .flatMap(channel -> {
                            this.game.getPlayers().values().forEach(DicePlayer::cancelBet);
                            return DiscordUtils.sendMessage(
                                    String.format(Emoji.CHECK_MARK + " Dice game cancelled by **%s**.",
                                            member.getUsername()), channel);
                        })
                        .then(Mono.fromRunnable(this.game::stop)));
    }

}
