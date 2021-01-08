/*
package com.shadorc.shadbot.command.game.trivia;

import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.Inputs;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import reactor.core.publisher.Mono;

public class TriviaInputs extends Inputs {

    private final TriviaGame game;

    private TriviaInputs(GatewayDiscordClient gateway, TriviaGame game) {
        super(gateway, game.getDuration(), game.getContext().getChannelId());
        this.game = game;
    }

    public static TriviaInputs create(GatewayDiscordClient gateway, TriviaGame game) {
        return new TriviaInputs(gateway, game);
    }

    @Override
    public Mono<Boolean> isValidEvent(MessageCreateEvent event) {
        return Mono.just(true);
    }

    @Override
    public boolean takeEventWile(MessageCreateEvent event) {
        return this.game.isScheduled();
    }

    @Override
    public Mono<Void> processEvent(MessageCreateEvent event) {
        return this.game.isCancelMessage(event.getMessage())
                .flatMap(isCancelMsg -> {
                    final Member member = event.getMember().orElseThrow();
                    if (isCancelMsg) {
                        return event.getMessage().getChannel()
                                .flatMap(channel -> DiscordUtils.sendMessage(
                                        String.format(Emoji.CHECK_MARK + " Trivia game cancelled by **%s**.",
                                                member.getUsername()), channel))
                                .then(Mono.fromRunnable(this.game::stop));
                    }

                    // It's a number or a text
                    final String content = event.getMessage().getContent();
                    final Integer choice = NumberUtils.toIntBetweenOrNull(content, 1, this.game.getAnswers().size());

                    // Message is a text and doesn't match any answers, ignore it
                    if (choice == null && this.game.getAnswers().stream().noneMatch(content::equalsIgnoreCase)) {
                        return Mono.empty();
                    }

                    // If the user has already answered and has been warned, ignore him
                    if (this.game.getPlayers().containsKey(member.getId())
                            && this.game.getPlayers().get(member.getId()).hasAnswered()) {
                        return Mono.empty();
                    }

                    final String answer = choice == null ? content : this.game.getAnswers().get(choice - 1);

                    if (this.game.getPlayers().containsKey(member.getId())) {
                        this.game.hasAnswered(member.getId());
                        return event.getMessage().getChannel()
                                .flatMap(channel -> DiscordUtils.sendMessage(
                                        String.format(Emoji.GREY_EXCLAMATION + " (**%s**) You can only answer once.",
                                                member.getUsername()), channel))
                                .then();
                    } else if (answer.equalsIgnoreCase(this.game.getCorrectAnswer())) {
                        return this.game.win(member).then();
                    } else {
                        this.game.hasAnswered(member.getId());
                        return event.getMessage().getChannel()
                                .flatMap(channel -> DiscordUtils.sendMessage(
                                        String.format(Emoji.THUMBSDOWN + " (**%s**) Wrong answer.",
                                                member.getUsername()), channel))
                                .then();
                    }
                });
    }

}
*/
