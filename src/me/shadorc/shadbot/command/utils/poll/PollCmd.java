// TODO
// package me.shadorc.shadbot.command.utils.poll;
//
// import java.util.List;
// import java.util.concurrent.ConcurrentHashMap;
// import java.util.stream.Collectors;
//
// import discord4j.core.object.util.Snowflake;
// import discord4j.core.spec.EmbedCreateSpec;
// import me.shadorc.shadbot.core.command.AbstractCommand;
// import me.shadorc.shadbot.core.command.CommandCategory;
// import me.shadorc.shadbot.core.command.CommandPermission;
// import me.shadorc.shadbot.core.command.Context;
// import me.shadorc.shadbot.core.command.annotation.Command;
// import me.shadorc.shadbot.core.command.annotation.RateLimited;
// import me.shadorc.shadbot.exception.CommandException;
// import me.shadorc.shadbot.utils.NumberUtils;
// import me.shadorc.shadbot.utils.StringUtils;
// import me.shadorc.shadbot.utils.embed.HelpBuilder;
// import reactor.core.publisher.Mono;
//
// @RateLimited
// @Command(category = CommandCategory.UTILS, names = { "poll" })
// public class PollCmd extends AbstractCommand {
//
// protected static final ConcurrentHashMap<Snowflake, PollManager> MANAGER = new ConcurrentHashMap<>();
//
// private static final int MIN_CHOICES_NUM = 2;
// private static final int MAX_CHOICES_NUM = 10;
// private static final int MIN_DURATION = 10;
// private static final int MAX_DURATION = 3600;
//
// @Override
// public void execute(Context context) {
// context.requireArg();
// context.getAuthorPermission().subscribe(perm -> {
// PollManager pollManager = MANAGER.get(context.getChannelId());
//
// // It's a cancel command
// if(this.isCancelMsg(context, perm, pollManager)) {
// pollManager.stop();
// }
//
// // It's a vote
// else if(pollManager != null) {
// Integer num = NumberUtils.asIntBetween(context.getArg().get(), 1, pollManager.getChoicesCount());
// if(num == null) {
// throw new CommandException(String.format("``%s` is not a valid number, must be between 1 and %d.",
// context.getArg(), pollManager.getChoicesCount()));
// }
// pollManager.vote(context.getAuthorId(), num);
// }
// // It's a poll creation
// else {
// pollManager = this.createPoll(context);
// if(MANAGER.putIfAbsent(context.getChannelId(), pollManager) == null) {
// pollManager.start();
// }
// }
// });
// }
//
// private boolean isCancelMsg(Context context, CommandPermission perm, PollManager pollManager) {
// final boolean isAuthor = context.getAuthorId().equals(pollManager.getContext().getAuthorId());
// final boolean isAdmin = perm.isSuperior(CommandPermission.USER);
// final boolean isCancelMsg = context.getArg().get().matches("stop|cancel");
// return pollManager != null && isCancelMsg && (isAuthor || isAdmin);
// }
//
// private PollManager createPoll(Context context) {
// List<String> args = context.requireArgs(2);
//
// PollCreateSpec spec = new PollCreateSpec();
//
// Integer duration = NumberUtils.asIntBetween(args.get(0), MIN_DURATION, MAX_DURATION);
// if(duration == null) {
// throw new CommandException(String.format("`%s` is not a valid duration, it must be between %ds and %ds.",
// args.get(0), MIN_DURATION, MAX_DURATION));
// }
// spec.setDuration(duration);
//
// List<String> substrings = StringUtils.getQuotedWords(args.get(1));
// if(substrings.isEmpty() || StringUtils.countMatches(args.get(1), "\"") % 2 != 0) {
// throw new CommandException("Question and choices cannot be empty and must be enclosed in quotation marks.");
// }
// spec.setQuestion(substrings.get(0));
//
// // Remove duplicate choices
// List<String> choices = substrings.subList(1, substrings.size()).stream().distinct().collect(Collectors.toList());
// if(!NumberUtils.isInRange(choices.size(), MIN_CHOICES_NUM, MAX_CHOICES_NUM)) {
// throw new CommandException(String.format("You must specify between %d and %d different non-empty choices.",
// MIN_CHOICES_NUM, MAX_CHOICES_NUM));
// }
// spec.setChoices(choices);
//
// return new PollManager(context, spec);
// }
//
// @Override
// public Mono<EmbedCreateSpec> getHelp(Context context) {
// return new HelpBuilder(this, context)
// .setDescription("Create a poll.")
// .addArg("duration", String.format("in seconds, must be between %ds and %ds (1 hour)",
// MIN_DURATION, MAX_DURATION), false)
// .addArg("\"question\"", false)
// .addArg("choice1", false)
// .addArg("choice2", false)
// .addArg("choiceX", true)
// .setExample(String.format("`%s%s 120 \"Where do we eat at noon?\" \"White\" \"53\" \"A dog\"`",
// context.getPrefix(), this.getName()))
// .addField("Restrictions", String.format("**question and choices** - must be in quotation marks"
// + "%n**choices** - min: %d, max: %d", MIN_CHOICES_NUM, MAX_CHOICES_NUM), false)
// .build();
// }
// }
