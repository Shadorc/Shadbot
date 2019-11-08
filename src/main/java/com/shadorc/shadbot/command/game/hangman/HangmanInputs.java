package com.shadorc.shadbot.command.game.hangman;

import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.Inputs;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import reactor.core.publisher.Mono;

import java.util.regex.Pattern;

public class HangmanInputs extends Inputs {

    private static final Pattern WORD_PATTERN = Pattern.compile("[a-z]+");

    private final HangmanGame game;

    public HangmanInputs(DiscordClient client, HangmanGame game) {
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
                .map(isCancelCmd -> isCancelCmd || this.game.getContext().getAuthorId().equals(member.getId()));
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
                                        String.format(Emoji.CHECK_MARK + " Hangman game cancelled by **%s**.",
                                                member.getUsername()), channel))
                                .then(Mono.fromRunnable(this.game::stop));
                    }

                    final String content = event.getMessage().getContent().orElseThrow().toLowerCase().trim();

                    // Check only if content is an unique word/letter
                    if (!WORD_PATTERN.matcher(content).matches()) {
                        return Mono.empty();
                    }

                    if (content.length() == 1
                            && !this.game.getRateLimiter().isLimitedAndWarn(this.game.getContext().getChannelId(), this.game.getContext().getMember())) {
                        return this.game.checkLetter(content);
                    } else if (content.length() == this.game.getWord().length()
                            && !this.game.getRateLimiter().isLimitedAndWarn(this.game.getContext().getChannelId(), this.game.getContext().getMember())) {
                        return this.game.checkWord(content);
                    }

                    return Mono.empty();
                });
    }

}
