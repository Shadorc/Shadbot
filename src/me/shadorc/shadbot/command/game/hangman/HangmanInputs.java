package me.shadorc.shadbot.command.game.hangman;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import me.shadorc.shadbot.listener.interceptor.Inputs;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import reactor.core.publisher.Mono;

public class HangmanInputs extends Inputs {

	private final HangmanManager manager;

	public HangmanInputs(DiscordClient client, HangmanManager manager) {
		super(client, manager.getDuration());
		this.manager = manager;
	}

	@Override
	public Mono<Boolean> isValidEvent(MessageCreateEvent event) {
		if(event.getMessage().getContent().isEmpty() || event.getMember().isEmpty()) {
			return Mono.just(false);
		}

		if(!event.getMessage().getChannelId().equals(this.manager.getContext().getChannelId())) {
			return Mono.just(false);
		}

		final Member member = event.getMember().get();
		return this.manager.isCancelMessage(event.getMessage())
				.map(isCancelCmd -> isCancelCmd || this.manager.getContext().getAuthorId().equals(member.getId()));
	}

	@Override
	public boolean takeEventWile(MessageCreateEvent ignored) {
		return this.manager.isScheduled();
	}

	@Override
	public Mono<Void> processEvent(MessageCreateEvent event) {
		return this.manager.isCancelMessage(event.getMessage())
				.flatMap(isCancelMsg -> {
					final Member member = event.getMember().get();
					if(isCancelMsg) {
						return event.getMessage().getChannel()
								.flatMap(channel -> DiscordUtils.sendMessage(
										String.format(Emoji.CHECK_MARK + " Hangman game cancelled by **%s**.",
												member.getUsername()), channel))
								.then(Mono.fromRunnable(this.manager::stop));
					}

					final String content = event.getMessage().getContent().get().toLowerCase().trim();

					// Check only if content is an unique word/letter
					if(!content.matches("[a-z]+")) {
						return Mono.empty();
					}

					if(content.length() == 1
							&& !this.manager.getRateLimiter().isLimitedAndWarn(this.manager.getContext().getChannelId(), this.manager.getContext().getMember())) {
						return this.manager.checkLetter(content);
					} else if(content.length() == this.manager.getWord().length()
							&& !this.manager.getRateLimiter().isLimitedAndWarn(this.manager.getContext().getChannelId(), this.manager.getContext().getMember())) {
						return this.manager.checkWord(content);
					}

					return Mono.empty();
				});
	}

}
