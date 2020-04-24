package com.shadorc.shadbot.command.utils.poll;

import com.google.common.collect.HashBiMap;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.message.ReactionMessage;
import com.shadorc.shadbot.utils.*;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.EmojiData;
import discord4j.discordjson.json.MessageData;
import discord4j.discordjson.json.ReactionData;
import discord4j.rest.entity.RestMessage;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;

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

        final Consumer<EmbedCreateSpec> embedConsumer = DiscordUtils.getDefaultEmbed()
                .andThen(embed -> embed.setAuthor(String.format("Poll by %s", this.context.getUsername()),
                        null, this.context.getAvatarUrl())
                        .setDescription(String.format("Vote by clicking on the corresponding number.%n%n__**%s**__%s",
                                this.spec.getQuestion(), representation))
                        .setFooter(String.format("You have %s to vote. Use %spoll cancel to cancel.",
                                FormatUtils.shortDuration(this.spec.getDuration().toMillis()), this.context.getPrefix()),
                                "https://i.imgur.com/jcrUDLY.png"));

        return this.voteMessage.send(embedConsumer)
                .flatMap(message -> Mono.delay(this.spec.getDuration(), Schedulers.boundedElastic())
                        .thenReturn(message.getId()))
                .map(messageId -> this.context.getClient()
                        .rest()
                        .getMessageById(this.context.getChannelId(), messageId))
                .flatMap(RestMessage::getData)
                .map(MessageData::reactions)
                .map(possible -> possible.toOptional().orElse(Collections.emptyList()))
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

    private Mono<Message> sendResults(List<ReactionData> reactionDataList) {
        // Reactions are not in the same order as they were when added to the message, they need to be ordered
        final Map<ReactionEmoji, String> reactionsChoices = HashBiMap.create(this.spec.getChoices()).inverse();
        Map<String, Integer> choicesVotes = new HashMap<>(reactionDataList.size());
        for (final ReactionData reactionData : reactionDataList) {
            final EmojiData emojiData = reactionData.emoji();
            final ReactionEmoji reactionEmoji = ReactionEmoji.of(
                    emojiData.id().map(Long::parseLong).orElse(null),
                    emojiData.name().orElse(null),
                    emojiData.animated().toOptional().<Boolean>map(Function.identity()).orElse(false));

            final String choice = reactionsChoices.get(reactionEmoji);
            // Ignore possible reactions added by users
            if (choice != null) {
                // -1 is here to ignore the reaction of the bot itself
                choicesVotes.put(choice, reactionData.count() - 1);
            }
        }

        // Sort votes map by value in the ascending order
        choicesVotes = Utils.sortMap(choicesVotes, Collections.reverseOrder(Entry.comparingByValue()));

        final StringBuilder representation = new StringBuilder();
        int count = 1;
        for (final Entry<String, Integer> entry : choicesVotes.entrySet()) {
            representation.append(String.format("%n\t**%d.** %s (%s)", count,
                    entry.getKey(), StringUtils.pluralOf(entry.getValue(), "vote")));
            count++;
        }

        if (representation.length() == 0) {
            representation.append("\nAll choices have been removed.");
        }

        final Consumer<EmbedCreateSpec> embedConsumer = DiscordUtils.getDefaultEmbed()
                .andThen(embed -> embed.setAuthor(String.format("Poll results (Author: %s)", this.context.getUsername()),
                        null, this.context.getAvatarUrl())
                        .setDescription(String.format("__**%s**__%s", this.spec.getQuestion(), representation)));

        return this.context.getChannel()
                .flatMap(channel -> DiscordUtils.sendMessage(embedConsumer, channel));
    }

}