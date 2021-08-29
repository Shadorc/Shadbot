package com.shadorc.shadbot.command.util.poll;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.ExceptionHandler;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.MapUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.Reaction;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.legacy.LegacyEmbedCreateSpec;
import discord4j.rest.http.client.ClientException;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

public class PollManager {

    private final PollCmd pollCmd;
    private final Context context;
    private final PollCreateSpec spec;

    public PollManager(PollCmd pollCmd, Context context, PollCreateSpec spec) {
        this.pollCmd = pollCmd;
        this.context = context;
        this.spec = spec;
    }

    public Mono<?> show() {
        final StringBuilder representation = new StringBuilder();
        for (int i = 0; i < this.spec.choices().size(); i++) {
            representation.append("\n\t**%d.** %s".formatted(i + 1, this.spec.choices().keySet().toArray()[i]));
        }

        final Consumer<LegacyEmbedCreateSpec> embedConsumer = ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor(this.context.localize("poll.title")
                                        .formatted(this.context.getAuthorName()),
                                null, this.context.getAuthorAvatar())
                        .setDescription(this.context.localize("poll.description")
                                .formatted(this.spec.question(), representation))
                        .setFooter(this.context.localize("poll.footer")
                                        .formatted(FormatUtil.formatDuration(this.spec.duration()), Emoji.RED_CROSS),
                                "https://i.imgur.com/jcrUDLY.png"));

        return this.context.createFollowupMessage(embedConsumer)
                .flatMap(message -> Flux.fromIterable(this.spec.choices().values())
                        .flatMap(message::addReaction)
                        .then(Mono.fromRunnable(() -> this.scheduleEnd(message.getId()))));
    }

    private void scheduleEnd(Snowflake messageId) {
        Mono.delay(this.spec.duration(), Schedulers.boundedElastic())
                .then(this.end(messageId))
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    private Mono<Message> end(Snowflake messageId) {
        return Mono.fromRunnable(() -> this.pollCmd.getManagers().remove(this.context.getCommandId()))
                .then(this.context.getClient()
                        .getMessageById(this.context.getChannelId(), messageId))
                .onErrorResume(ClientException.isStatusCode(HttpResponseStatus.FORBIDDEN.code()),
                        err -> Mono.empty())
                .map(Message::getReactions)
                .flatMap(this::sendResults);
    }

    private Mono<Message> sendResults(List<Reaction> reactionSet) {
        final Map<ReactionEmoji, String> reactionsChoices = MapUtil.inverse(this.spec.choices());

        final Map<String, Integer> choiceVoteMap = new HashMap<>(reactionsChoices.size());
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
                MapUtil.sort(choiceVoteMap, Collections.reverseOrder(Entry.comparingByValue()));

        final StringBuilder representation = new StringBuilder();
        int count = 1;
        for (final Entry<String, Integer> entry : choiceVoteOrderedMap.entrySet()) {
            representation.append(this.context.localize("poll.choice.result")
                    .formatted(count, entry.getKey(), this.context.localize(entry.getValue())));
            count++;
        }

        if (representation.isEmpty()) {
            representation.append(this.context.localize("poll.choices.removed"));
        }

        final Consumer<LegacyEmbedCreateSpec> embedConsumer = ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor(this.context.localize("poll.results.title"),
                                null, this.context.getAuthorAvatar())
                        .setDescription("__**%s**__%s".formatted(this.spec.question(), representation))
                        .setFooter(this.context.localize("poll.results.footer")
                                .formatted(this.context.getAuthorName()), null));

        return this.context.createFollowupMessage(embedConsumer);
    }

}
