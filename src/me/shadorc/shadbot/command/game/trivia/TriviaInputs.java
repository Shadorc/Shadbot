package me.shadorc.shadbot.command.game.trivia;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import me.shadorc.shadbot.listener.interceptor.Inputs;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import reactor.core.publisher.Mono;

public class TriviaInputs extends Inputs {

	private final TriviaManager manager;

	public TriviaInputs(DiscordClient client, TriviaManager manager) {
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
				.map(isCancelCmd -> isCancelCmd || (this.manager.getPlayers().containsKey(member.getId())));
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
										String.format(Emoji.CHECK_MARK + " Trivia game cancelled by **%s**.",
												member.getUsername()), channel))
								.then(Mono.fromRunnable(this.manager::stop));
					}

					// It's a number or a text
					final String content = event.getMessage().getContent().get();
					final Integer choice = NumberUtils.asIntBetween(content, 1, this.manager.getAnswers().size());

					// Message is a text and doesn't match any answers, ignore it
					if(choice == null && !this.manager.getAnswers().stream().anyMatch(content::equalsIgnoreCase)) {
						return Mono.empty();
					}

					// If the user has already answered and has been warned, ignore him
					if(this.manager.getPlayers().get(member.getId()).hasAnswered()) {
						return Mono.empty();
					}

					final String answer = choice == null ? content : this.manager.getAnswers().get(choice - 1);

					if(this.manager.getPlayers().containsKey(member.getId())) {
						this.manager.hasAnswered(member.getId());
						return event.getMessage().getChannel()
								.flatMap(channel -> DiscordUtils.sendMessage(
										String.format(Emoji.GREY_EXCLAMATION + " (**%s**) You can only answer once.",
												member.getUsername()), channel))
								.then();
					} else if(answer.equalsIgnoreCase(this.manager.getCorrectAnswer())) {
						return this.manager.win(member).then();
					} else {
						this.manager.hasAnswered(member.getId());
						return event.getMessage().getChannel()
								.flatMap(channel -> DiscordUtils.sendMessage(
										String.format(Emoji.THUMBSDOWN + " (**%s**) Wrong answer.",
												member.getUsername()), channel))
								.then();
					}
				});
	}

}
