package com.locibot.locibot.command.game.trivia;

import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.object.inputs.MessageInputs;
import com.locibot.locibot.utils.NumberUtil;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import reactor.core.publisher.Mono;

public class TriviaInputs extends MessageInputs {

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
    public Mono<?> processEvent(MessageCreateEvent event) {
        // It's a number or a text
        final String content = event.getMessage().getContent();
        final Integer choice = NumberUtil.toIntBetweenOrNull(content, 1, this.game.getAnswers().size());

        // Message is a text and doesn't match any answers, ignore it
        if (choice == null && this.game.getAnswers().stream().noneMatch(content::equalsIgnoreCase)) {
            return Mono.empty();
        }

        // If the user has already answered and has been warned, ignore him
        final Member member = event.getMember().orElseThrow();
        final TriviaPlayer player = this.game.getPlayers().get(member.getId());
        if (player != null && player.hasAnswered()) {
            return Mono.empty();
        }

        this.game.hasAnswered(member.getId());

        final String answer = choice == null ? content : this.game.getAnswers().get(choice - 1);
        // The user was already a player, so they already guessed something, but was not yet warned
        if (player != null) {
            return this.game.getContext()
                    .createFollowupMessage(Emoji.GREY_EXCLAMATION, this.game.getContext().localize("trivia.already.answered")
                            .formatted(member.getUsername()));
        } else if (answer.equalsIgnoreCase(this.game.getCorrectAnswer())) {
            return this.game.win(member);
        } else {
            return this.game.getContext()
                    .createFollowupMessage(Emoji.THUMBSDOWN, this.game.getContext().localize("trivia.wrong.answer")
                            .formatted(member.getUsername()));
        }
    }

}