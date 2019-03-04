package me.shadorc.shadbot.command.utils.poll;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import reactor.core.publisher.Mono;

public class PollCmd extends BaseCmd {

	private static final List<String> NUMBER_UNICODE = List.of(
			"\u0030\u20E3", "\u0031\u20E3", "\u0032\u20E3", "\u0033\u20E3", "\u0034\u20E3",
			"\u0035\u20E3", "\u0036\u20E3", "\u0037\u20E3", "\u0038\u20E3", "\u0039\u20E3",
			"\u0040\u20E3");

	private static final int MIN_CHOICES_NUM = 2;
	private static final int MAX_CHOICES_NUM = 10;
	private static final int MIN_DURATION = 10;
	private static final int MAX_DURATION = 3600;

	private final Map<Snowflake, PollManager> managers;

	public PollCmd() {
		super(CommandCategory.UTILS, List.of("poll"));
		this.setDefaultRateLimiter();

		this.managers = new ConcurrentHashMap<>();
	}

	@Override
	public Mono<Void> execute(Context context) {
		context.requireArg();

		return context.getChannel()
				.flatMap(channel -> DiscordUtils.requirePermissions(channel, Permission.ADD_REACTIONS))
				.then(context.getPermission())
				.doOnNext(permission -> {
					final PollManager pollManager = this.managers.computeIfAbsent(context.getChannelId(),
							channelId -> {
								final PollManager manager = this.createPoll(context);
								manager.start();
								return manager;
							});

					if(this.isCancelMsg(context, permission, pollManager)) {
						pollManager.stop();
					}
				})
				.then();
	}

	private boolean isCancelMsg(Context context, CommandPermission perm, PollManager pollManager) {
		final boolean isAuthor = context.getAuthorId().equals(pollManager.getContext().getAuthorId());
		final boolean isAdmin = perm.equals(CommandPermission.ADMIN);
		final boolean isCancelMsg = context.getArg().map(arg -> arg.matches("stop|cancel")).orElse(false);
		return isCancelMsg && (isAuthor || isAdmin);
	}

	private PollManager createPoll(Context context) {
		final List<String> args = context.requireArgs(2);

		final int countMatches = StringUtils.countMatches(args.get(1), "\"");
		if(countMatches == 0 || countMatches % 2 != 0) {
			throw new CommandException("Question and choices must be enclosed in quotation marks.");
		}

		Integer seconds;
		if(NumberUtils.isPositiveLong(args.get(0))) {
			seconds = NumberUtils.asIntBetween(args.get(0), MIN_DURATION, MAX_DURATION);
			if(seconds == null) {
				throw new CommandException(String.format("`%s` is not a valid duration, it must be between %ds and %ds.",
						args.get(0), MIN_DURATION, MAX_DURATION));
			}
		} else {
			try {
				seconds = Integer.valueOf((int) TimeUtils.parseTime(args.get(0)));
			} catch (final IllegalArgumentException err) {
				throw new CommandException(err.getMessage());
			}
			if(!NumberUtils.isInRange(seconds, MIN_DURATION, MAX_DURATION)) {
				throw new CommandException(String.format("`%s` is not a valid duration, it must be between %ds and %ds.",
						args.get(0), MIN_DURATION, MAX_DURATION));
			}
		}

		final List<String> substrings = Arrays.asList(StringUtils.substringsBetween(args.get(1), "\"", "\""));
		if(substrings.size() != substrings.stream().filter(str -> !str.isBlank()).count()) {
			throw new CommandException("Question or choice cannot be blank.");
		}

		// Remove duplicate choices
		final List<String> choices = substrings.stream()
				.skip(1)
				.distinct()
				.collect(Collectors.toList());
		if(!NumberUtils.isInRange(choices.size(), MIN_CHOICES_NUM, MAX_CHOICES_NUM)) {
			throw new CommandException(String.format("You must specify between %d and %d different non-empty choices and one question.",
					MIN_CHOICES_NUM, MAX_CHOICES_NUM));
		}

		final Map<String, ReactionEmoji> choicesReactions = new HashMap<>();
		for(int i = 0; i < choices.size(); i++) {
			choicesReactions.put(choices.get(i), ReactionEmoji.unicode(NUMBER_UNICODE.get(i + 1)));
		}

		return new PollManager(this, context, new PollCreateSpec(Duration.ofSeconds(seconds), substrings.get(0), choicesReactions));
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Create a poll.")
				.addArg("duration", false)
				.addArg("\"question\"", false)
				.addArg("\"choice1\"", false)
				.addArg("\"choice2\"", false)
				.addArg("\"choiceX\"", true)
				.setExample(String.format("`%s%s 120 \"Where do we eat at noon?\" \"White\" \"53\" \"A dog\"`",
						context.getPrefix(), this.getName()))
				.addField("Restrictions", String.format("%n**duration** - in seconds, must be between %ds and %ds (1 hour)"
						+ "%n**question** - must be in quotation marks"
						+ "%n**choices** - must be in quotation marks, min: %d, max: %d",
						MIN_DURATION, MAX_DURATION, MIN_CHOICES_NUM, MAX_CHOICES_NUM), false)
				.build();
	}

	public Map<Snowflake, PollManager> getManagers() {
		return this.managers;
	}
}
