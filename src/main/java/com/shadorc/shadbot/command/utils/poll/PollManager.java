/*
package com.shadorc.shadbot.command.utils.poll;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.ExceptionHandler;
import com.shadorc.shadbot.object.message.ReactionMessage;
import com.shadorc.shadbot.utils.*;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.Reaction;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.http.client.ClientException;
import io.netty.handler.codec.http.HttpResponseStatus;
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

    public Mono<Void> start() {
        this.schedule(Mono.fromRunnable(this::stop), this.spec.getDuration());
        return this.show();
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

        final Consumer<EmbedCreateSpec> embedConsumer = ShadbotUtils.getDefaultEmbed()
                .andThen(embed -> embed.setAuthor(String.format("Poll by %s", this.context.getUsername()),
                        null, this.context.getAvatarUrl())
                        .setDescription(String.format("Vote by clicking on the corresponding number.%n%n__**%s**__%s",
                                this.spec.getQuestion(), representation))
                        .setFooter(String.format("You have %s to vote. Use %spoll cancel to cancel.",
                                FormatUtils.formatDuration(this.spec.getDuration()), this.context.getPrefix()),
                                "https://i.imgur.com/jcrUDLY.png"));

        return this.voteMessage.send(embedConsumer)
                .flatMap(message -> Mono.delay(this.spec.getDuration(), Schedulers.boundedElastic())
                        .thenReturn(message.getId()))
                .flatMap(messageId -> this.context.getClient()
                        .getMessageById(this.context.getChannelId(), messageId))
                .onErrorResume(ClientException.isStatusCode(HttpResponseStatus.FORBIDDEN.code()), err -> Mono.empty())
                .map(Message::getReactions)
                .flatMap(this::sendResults)
                .then();
    }

    private <T> void schedule(Mono<T> mono, Duration duration) {
        this.cancelScheduledTask();
        this.scheduledTask = Mono.delay(duration, Schedulers.boundedElastic())
                .then(mono)
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    private void cancelScheduledTask() {
        if (this.scheduledTask != null) {
            this.scheduledTask.dispose();
        }
    }

    public Context getContext() {
        return this.context;
    }

    private Mono<Message> sendResults(Set<Reaction> reactionSet) {
        // Reactions are not in the same order as they were when added to the message, they need to be ordered
        final Map<ReactionEmoji, String> reactionsChoices = MapUtils.inverse(this.spec.getChoices());
        final Map<String, Integer> choiceVoteMap = new HashMap<>(reactionSet.size());
        for (final Reaction reaction : reactionSet) {
            final String choice = reactionsChoices.get(reaction.getEmoji());
            // Ignore possible reactions added by users
            if (choice != null) {
                // -1 is here to ignore the reaction of the bot itself
                choiceVoteMap.put(choice, reaction.getCount() - 1);
            }
        }

        // Sort votes map by value in the ascending order
        final Map<String, Integer> choiceVoteOrderedMap =
                MapUtils.sort(choiceVoteMap, Collections.reverseOrder(Entry.comparingByValue()));

        final StringBuilder representation = new StringBuilder();
        int count = 1;
        for (final Entry<String, Integer> entry : choiceVoteOrderedMap.entrySet()) {
            representation.append(String.format("%n\t**%d.** %s (%s)", count,
                    entry.getKey(), StringUtils.pluralOf(entry.getValue(), "vote")));
            count++;
        }

        if (representation.isEmpty()) {
            representation.append("\nAll choices have been removed.");
        }

        final Consumer<EmbedCreateSpec> embedConsumer = ShadbotUtils.getDefaultEmbed()
                .andThen(embed -> embed.setAuthor(String.format("Poll results (Author: %s)", this.context.getUsername()),
                        null, this.context.getAvatarUrl())
                        .setDescription(String.format("__**%s**__%s", this.spec.getQuestion(), representation)));

        return this.context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(embedConsumer, channel));
    }

}*/
