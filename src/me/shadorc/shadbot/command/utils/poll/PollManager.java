package me.shadorc.shadbot.command.utils.poll;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.Reaction;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.object.message.ReactionMessage;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.exception.ExceptionHandler;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

public class PollManager {

    private final PollCmd pollCmd;
    private final Context context;
    private final PollCreateSpec spec;
    private final ReactionMessage voteMessage;
    private Disposable scheduledTask;

    public PollManager(PollCmd pollCmd, Context context, PollCreateSpec spec) {
        this.pollCmd = pollCmd;
        this.context = context;
        this.spec = spec;
        this.voteMessage = new ReactionMessage(context.getClient(), context.getChannelId(), spec.getReactions());
        this.scheduledTask = null;
    }

    public void start() {
        this.schedule(Mono.fromRunnable(this::stop), this.spec.getDuration());
        this.show()
                .subscribe(null, err -> ExceptionHandler.handleUnknownError(this.context.getClient(), err));
    }

    public void stop() {
        this.cancelScheduledTask();
        this.pollCmd.getManagers().remove(this.context.getChannelId());
    }

    private Mono<Void> show() {
        final StringBuilder representation = new StringBuilder();
        for (int i = 0; i < this.spec.getChoices().size(); i++) {
            representation.append(String.format("%n\t**%d.** %s", i + 1, this.spec.getChoices().keySet().toArray()[i]));
        }

        final Consumer<EmbedCreateSpec> embedConsumer = EmbedUtils.getDefaultEmbed()
                .andThen(embed -> embed.setAuthor(String.format("Poll by %s", this.context.getUsername()),
                        null, this.context.getAvatarUrl())
                        .setDescription(String.format("Vote by clicking on the corresponding number.%n%n__**%s**__%s",
                                this.spec.getQuestion(), representation.toString()))
                        .setFooter(String.format("You have %s to vote.",
                                FormatUtils.shortDuration(this.spec.getDuration().toMillis())),
                                "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1d/Clock_simple_white.svg/2000px-Clock_simple_white.svg.png"));

        return this.voteMessage.send(embedConsumer)
                .flatMap(message -> Mono.delay(this.spec.getDuration(), Schedulers.elastic())
                        .thenReturn(message.getId()))
                .flatMap(messageId -> this.context.getClient().getMessageById(this.context.getChannelId(), messageId))
                .map(Message::getReactions)
                .flatMap(this::sendResults)
                .then();
    }

    private <T> void schedule(Mono<T> mono, Duration duration) {
        this.cancelScheduledTask();
        this.scheduledTask = Mono.delay(duration, Schedulers.elastic())
                .then(mono)
                .subscribe(null, err -> ExceptionHandler.handleUnknownError(this.context.getClient(), err));
    }

    private void cancelScheduledTask() {
        if (this.scheduledTask != null) {
            this.scheduledTask.dispose();
        }
    }

    public Context getContext() {
        return this.context;
    }

    private Mono<Message> sendResults(Set<Reaction> reactions) {
        // Reactions are not in the same order as they were when added to the message, they need to be ordered
        final BiMap<ReactionEmoji, String> reactionsChoices = HashBiMap.create(this.spec.getChoices()).inverse();
        final Map<String, Integer> choicesVotes = new HashMap<>();
        for (final Reaction reaction : reactions) {
            // -1 is here to ignore the reaction of the bot itself
            choicesVotes.put(reactionsChoices.get(reaction.getEmoji()), reaction.getCount() - 1);
        }

        // Sort votes map by value in the ascending order
        final StringBuilder representation = new StringBuilder();
        int count = 1;
        for (final String key : Utils.sortByValue(choicesVotes, Collections.reverseOrder(Entry.comparingByValue())).keySet()) {
            representation.append(String.format("%n\t**%d.** %s (Votes: %d)", count, key, choicesVotes.get(key)));
            count++;
        }

        final Consumer<EmbedCreateSpec> embedConsumer = EmbedUtils.getDefaultEmbed()
                .andThen(embed -> embed.setAuthor(String.format("Poll results (Author: %s)", this.context.getUsername()),
                        null, this.context.getAvatarUrl())
                        .setDescription(String.format("__**%s**__%s", this.spec.getQuestion(), representation.toString())));

        return this.context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(embedConsumer, channel));
    }

}