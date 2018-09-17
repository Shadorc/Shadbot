package me.shadorc.shadbot.command.utils.poll;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.Reaction;
import discord4j.core.object.reaction.ReactionEmoji;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.game.AbstractGameManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.message.ReactionMessage;
import reactor.core.publisher.Mono;

public class PollManager extends AbstractGameManager {

	private final PollCreateSpec spec;
	private final ReactionMessage voteMessage;

	public PollManager(Context context, PollCreateSpec spec) {
		super(context);
		this.spec = spec;
		this.voteMessage = new ReactionMessage(context.getClient(), context.getChannelId(), spec.getDuration(), spec.getChoices().values());
	}

	@Override
	public void start() {
		this.schedule(Mono.fromRunnable(this::stop), this.spec.getDuration(), ChronoUnit.SECONDS);
		this.show().subscribe();
	}

	@Override
	public void stop() {
		this.cancelScheduledTask();
		PollCmd.MANAGER.remove(this.getContext().getChannelId());
	}

	@Override
	public Mono<Void> show() {
		return this.getContext().getAvatarUrl()
				.map(avatarUrl -> {
					final StringBuilder representation = new StringBuilder();
					for(int i = 0; i < this.spec.getChoices().size(); i++) {
						representation.append(String.format("%n\t**%d.** %s", i + 1, this.spec.getChoices().keySet().toArray()[i]));
					}

					return EmbedUtils.getDefaultEmbed()
							.setAuthor(String.format("Poll (Author: %s)", this.getContext().getUsername()), null, avatarUrl)
							.setDescription(String.format("Vote using: `%s%s <choice>`%n%n__**%s**__%s",
									this.getContext().getPrefix(), this.getContext().getCommandName(),
									this.spec.getQuestion(), representation.toString()))
							.setFooter(String.format("You have %s to vote.",
									FormatUtils.shortDuration(TimeUnit.SECONDS.toMillis(this.spec.getDuration()))),
									"https://upload.wikimedia.org/wikipedia/commons/thumb/1/1d/Clock_simple_white.svg/2000px-Clock_simple_white.svg.png");
				})
				.flatMap(this.voteMessage::sendMessage)
				.flatMap(this::sendResults)
				.then();
	}

	private Mono<Message> sendResults(Set<Reaction> reactions) {
		return this.getContext().getAvatarUrl()
				.map(avatarUrl -> {
					// Reactions are not in the same order as they were when added to the message, they need to be ordered
					final BiMap<ReactionEmoji, String> reactionsChoices = HashBiMap.create(this.spec.getChoices()).inverse();
					final Map<String, Integer> choicesVotes = new HashMap<>();
					for(Reaction reaction : reactions) {
						choicesVotes.put(reactionsChoices.get(reaction.getEmoji()), reaction.getCount());
					}

					// Sort votes map by value in the ascending order
					final StringBuilder representation = new StringBuilder();
					int count = 1;
					for(String key : Utils.sortByValue(choicesVotes, Collections.reverseOrder(Entry.comparingByValue())).keySet()) {
						representation.append(String.format("%n\t**%d.** %s (Votes: %d)", count, key, choicesVotes.get(key)));
						count++;
					}

					return EmbedUtils.getDefaultEmbed()
							.setAuthor(String.format("Poll results (Author: %s)", this.getContext().getUsername()), null, avatarUrl)
							.setDescription(String.format("__**%s**__%s", this.spec.getQuestion(), representation.toString()));

				})
				.flatMap(embed -> BotUtils.sendMessage(embed, this.getContext().getChannel()));
	}

}