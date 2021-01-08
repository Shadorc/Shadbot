/*
package com.shadorc.shadbot.command.game.roulette;

import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.Inputs;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class RouletteInputs extends Inputs {

    private final RouletteGame game;

    private RouletteInputs(GatewayDiscordClient gateway, RouletteGame game) {
        super(gateway, game.getDuration(), game.getContext().getChannelId());
        this.game = game;
    }

    public static RouletteInputs create(GatewayDiscordClient gateway, RouletteGame game) {
        return new RouletteInputs(gateway, game);
    }

    @Override
    public Mono<Boolean> isValidEvent(MessageCreateEvent event) {
        final Member member = event.getMember().orElseThrow();
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
                .map(Member::getUsername)
                .filterWhen(username -> this.game.isCancelMessage(event.getMessage()))
                .flatMap(username -> Flux.fromIterable(this.game.getPlayers().values())
                        .flatMap(RoulettePlayer::cancelBet)
                        .then()
                        .thenReturn(username))
                .map(username -> String.format(Emoji.CHECK_MARK + " Roulette game cancelled by **%s**.", username))
                .flatMap(text -> event.getMessage().getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(text, channel))
                        .then(Mono.fromRunnable(this.game::stop)));
    }

}
*/
