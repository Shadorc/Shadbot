package me.shadorc.shadbot.command.game.blackjack;

import java.util.function.Consumer;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.listener.interceptor.Inputs;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import reactor.core.publisher.Mono;

public class BlackjackInputs extends Inputs {

	private final BlackjackManager manager;

	public BlackjackInputs(DiscordClient client, BlackjackManager manager) {
		super(client, manager.getDuration());
		this.manager = manager;
	}

	@Override
	public Mono<Boolean> isValidEvent(MessageCreateEvent event) {
		if(event.getMessage().getContent().isEmpty() || event.getMember().isEmpty()) {
			return Mono.just(false);
		}

		final Member member = event.getMember().get();
		final String content = event.getMessage().getContent().get();

		if(!event.getMessage().getChannelId().equals(this.manager.getContext().getChannelId())) {
			return Mono.just(false);
		}

		return this.manager.isCancelMessage(event.getMessage())
				.map(isCancelCmd -> isCancelCmd || (this.manager.getPlayers().containsKey(member.getId())
						&& this.manager.getActions().containsKey(content)
						&& !this.manager.getRateLimiter().isLimitedAndWarn(event.getMessage().getChannelId(), member)));
	}

	@Override
	public boolean takeEventWile(MessageCreateEvent ignored) {
		return this.manager.isScheduled();
	}

	@Override
	public Mono<Void> processEvent(MessageCreateEvent event) {
		final Member member = event.getMember().get();
		// TODO
		if(this.manager.isCancelMessage(event.getMessage()).block()) {
			return event.getMessage().getChannel()
					.flatMap(channel -> DiscordUtils.sendMessage(
							String.format(Emoji.CHECK_MARK + " Blackjack game cancelled by **%s**.", member.getUsername()), channel))
					.then(Mono.fromRunnable(this.manager::stop));
		}

		final BlackjackPlayer player = this.manager.getPlayers().get(member.getId());

		if(player.isStanding()) {
			return this.manager.getContext().getChannel()
					.flatMap(channel -> DiscordUtils.sendMessage(
							String.format(Emoji.GREY_EXCLAMATION + " (**%s**) You're standing, you can't play anymore.",
									member.getUsername()), channel))
					.then();
		}

		final String prefix = Shadbot.getDatabase().getDBGuild(member.getGuildId()).getPrefix();
		final String content = event.getMessage().getContent().orElse("").replace(prefix, "").toLowerCase().trim();
		if("double down".equals(content) && player.getHand().count() != 2) {
			return this.manager.getContext().getChannel()
					.flatMap(channel -> DiscordUtils.sendMessage(
							String.format(Emoji.GREY_EXCLAMATION + " (**%s**) You must have a maximum of 2 cards to use `double down`.",
									member.getUsername()), channel))
					.then();
		}

		final Consumer<BlackjackPlayer> action = this.manager.getActions().get(content);
		if(action == null) {
			return Mono.empty();
		}

		action.accept(player);

		if(this.manager.allPlayersStanding()) {
			return this.manager.end();
		}
		return this.manager.show();
	}

}
