package com.locibot.locibot.command.util.poll;

import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.utils.DiscordUtil;
import com.locibot.locibot.utils.NumberUtil;
import com.locibot.locibot.utils.TimeUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.rest.util.ApplicationCommandOptionType;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PollCmd extends BaseCmd {

    private static final List<String> NUMBER_UNICODE = List.of(
            "\u0031\u20E3", "\u0032\u20E3", "\u0033\u20E3", "\u0034\u20E3", "\u0035\u20E3",
            "\u0036\u20E3", "\u0037\u20E3", "\u0038\u20E3", "\u0039\u20E3", "\uD83D\uDD1F");

    private static final int MIN_CHOICES_NUM = 2;
    private static final int MAX_CHOICES_NUM = 10;
    private static final int MIN_DURATION = 10;
    private static final int MAX_DURATION = 3600;

    private final Map<Snowflake /*CommandId*/, PollManager> managers;

    public PollCmd() {
        super(CommandCategory.UTILS, "poll", "Create a poll");
        this.managers = new ConcurrentHashMap<>();

        this.addOption("duration", "Number of seconds or formatted time (e.g. 72 or 1m12s), 1h max",
                true, ApplicationCommandOptionType.STRING);
        this.addOption("question", "The question to ask", true, ApplicationCommandOptionType.STRING);
        this.addOption("choice1", "The first choice", true, ApplicationCommandOptionType.STRING);
        this.addOption("choice2", "The second choice", true, ApplicationCommandOptionType.STRING);
        this.addOption("choice3", "The third choice", false, ApplicationCommandOptionType.STRING);
        this.addOption("choice4", "The fourth choice", false, ApplicationCommandOptionType.STRING);
        this.addOption("choice5", "The fifth choice", false, ApplicationCommandOptionType.STRING);
        this.addOption("choice6", "The sixth choice", false, ApplicationCommandOptionType.STRING);
        this.addOption("choice7", "The seventh choice", false, ApplicationCommandOptionType.STRING);
        this.addOption("choice8", "The eighth choice", false, ApplicationCommandOptionType.STRING);
        this.addOption("choice9", "The ninth choice", false, ApplicationCommandOptionType.STRING);
        this.addOption("choice10", "The tenth choice", false, ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.getChannel()
                .flatMap(channel -> DiscordUtil.requirePermissions(channel, Permission.ADD_REACTIONS))
                .thenReturn(this.createPoll(context))
                .doOnNext(pollManager -> this.managers.put(context.getEvent().getCommandId(), pollManager))
                .flatMap(PollManager::show)
                .doOnError(__ -> this.managers.remove(context.getEvent().getCommandId()));
    }

    private PollManager createPoll(Context context) {
        final String durationStr = context.getOptionAsString("duration").orElseThrow();
        final Duration duration;
        try {
            duration = TimeUtil.parseTime(durationStr);
            if (!NumberUtil.isBetween(duration.toSeconds(), MIN_DURATION, MAX_DURATION)) {
                throw new IllegalArgumentException();
            }
        } catch (final IllegalArgumentException err) {
            throw new CommandException(context.localize("poll.invalid.time")
                    .formatted(durationStr, context.localize(MIN_DURATION), context.localize(MAX_DURATION)));
        }

        final String question = context.getOptionAsString("question").orElseThrow();
        final List<String> choices = IntStream.range(1, MAX_CHOICES_NUM + 1).boxed()
                .map(index -> context.getOptionAsString("choice%d".formatted(index)).orElse(""))
                .filter(Predicate.not(String::isBlank))
                .distinct()
                .collect(Collectors.toList());

        if (choices.size() < MIN_CHOICES_NUM) {
            throw new CommandException(context.localize("poll.exception.min.choices"));
        }

        final Map<String, ReactionEmoji> choicesReactions = new LinkedHashMap<>();
        for (int i = 0; i < choices.size(); i++) {
            choicesReactions.put(choices.get(i), ReactionEmoji.unicode(NUMBER_UNICODE.get(i)));
        }

        return new PollManager(this, context, new PollCreateSpec(duration, question, choicesReactions));
    }

    public Map<Snowflake, PollManager> getManagers() {
        return this.managers;
    }
}
