package com.shadorc.shadbot.command.game.blackjack;

import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.Inputs;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public class BlackjackInputs extends Inputs {

    private final BlackjackGame game;

    public BlackjackInputs(DiscordClient client, BlackjackGame game) {
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
        final String content = event.getMessage().getContent().get();
        return this.game.isCancelMessage(event.getMessage())
                .map(isCancelCmd -> isCancelCmd || this.game.getPlayers().containsKey(member.getId())
                        && this.game.getActions().containsKey(content)
                        && !this.game.getRateLimiter().isLimitedAndWarn(event.getMessage().getChannelId(), member));
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
                        this.game.getPlayers().values().forEach(BlackjackPlayer::cancelBet);
                        return event.getMessage().getChannel()
                                .flatMap(channel -> DiscordUtils.sendMessage(
                                        String.format(Emoji.CHECK_MARK + " Blackjack game cancelled by **%s**.",
                                                member.getUsername()), channel))
                                .then(Mono.fromRunnable(this.game::stop));
                    }

                    final BlackjackPlayer player = this.game.getPlayers().get(member.getId());

                    if (player.isStanding()) {
                        return this.game.getContext().getChannel()
                                .flatMap(channel -> DiscordUtils.sendMessage(
                                        String.format(Emoji.GREY_EXCLAMATION + " (**%s**) You're standing, you can't play anymore.",
                                                member.getUsername()), channel))
                                .then();
                    }

                    final String prefix = DatabaseManager.getGuilds().getDBGuild(member.getGuildId()).getSettings().getPrefix();
                    final String content = event.getMessage().getContent().orElse("").replace(prefix, "").toLowerCase().trim();
                    if ("double down".equals(content) && player.getHand().count() != 2) {
                        return this.game.getContext().getChannel()
                                .flatMap(channel -> DiscordUtils.sendMessage(
                                        String.format(Emoji.GREY_EXCLAMATION + " (**%s**) You must have a maximum of 2 cards to use `double down`.",
                                                member.getUsername()), channel))
                                .then();
                    }

                    final Consumer<BlackjackPlayer> action = this.game.getActions().get(content);
                    if (action == null) {
                        return Mono.empty();
                    }

                    action.accept(player);

                    if (this.game.areAllPlayersStanding()) {
                        return this.game.end();
                    }
                    return this.game.show();
                });
    }

}
