// package me.shadorc.shadbot.command.utils.poll;
//
// import java.util.List;
// import java.util.concurrent.ConcurrentHashMap;
// import java.util.stream.Collectors;
//
// import discord4j.core.object.util.Snowflake;
// import me.shadorc.shadbot.core.command.AbstractCommand;
// import me.shadorc.shadbot.core.command.CommandCategory;
// import me.shadorc.shadbot.core.command.CommandPermission;
// import me.shadorc.shadbot.core.command.Context;
// import me.shadorc.shadbot.core.command.annotation.Command;
// import me.shadorc.shadbot.core.command.annotation.RateLimited;
// import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
// import me.shadorc.shadbot.exception.MissingArgumentException;
// import me.shadorc.shadbot.utils.NumberUtils;
// import me.shadorc.shadbot.utils.StringUtils;
// import me.shadorc.shadbot.utils.Utils;
// import me.shadorc.shadbot.utils.embed.HelpBuilder;
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
// public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
// context.requireArg();
//
// PollManager pollManager = MANAGER.get(context.getChannelId());
//
// if(context.getArg().get().matches("stop|cancel")
// && pollManager != null
// && (context.getAuthor().equals(pollManager.getAuthor()) || context.getAuthorPermission().isSuperior(CommandPermission.USER))) {
// pollManager.stop();
// return;
// }
//
// if(pollManager != null) {
// Integer num = NumberUtils.asIntBetween(context.getArg().get(), 1, pollManager.getChoicesCount());
// if(num == null) {
// throw new IllegalCmdArgumentException(String.format("``%s` is not a valid number, must be between 1 and %d.",
// context.getArg(), pollManager.getChoicesCount()));
// }
// pollManager.vote(context.getAuthor(), num);
// return;
// }
//
// pollManager = this.createPoll(context);
//
// if(MANAGER.putIfAbsent(context.getChannelId(), pollManager) == null) {
// pollManager.start();
// }
// }
//
// private PollManager createPoll(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
// List<String> splitArgs = StringUtils.split(context.getArg(), 2);
// if(splitArgs.size() != 2) {
// throw new MissingArgumentException();
// }
//
// Integer duration = NumberUtils.asIntBetween(splitArgs.get(0), MIN_DURATION, MAX_DURATION);
// if(duration == null) {
// throw new IllegalCmdArgumentException(String.format("`%s` is not a valid duration, it must be between %ds and %ds.",
// splitArgs.get(0), MIN_DURATION, MAX_DURATION));
// }
//
// List<String> substrings = StringUtils.getQuotedWords(splitArgs.get(1));
// if(substrings.isEmpty() || StringUtils.countMatches(splitArgs.get(1), "\"") % 2 != 0) {
// throw new IllegalCmdArgumentException("Question and choices cannot be empty and must be enclosed in quotation marks.");
// }
//
// // Remove duplicate choices
// List<String> choicesList = substrings.subList(1, substrings.size()).stream().distinct().collect(Collectors.toList());
// if(!Utils.isInRange(choicesList.size(), MIN_CHOICES_NUM, MAX_CHOICES_NUM)) {
// throw new IllegalCmdArgumentException(String.format("You must specify between %d and %d different non-empty choices.",
// MIN_CHOICES_NUM, MAX_CHOICES_NUM));
// }
//
// return new PollManager(this, context.getPrefix(), context.getChannel(), context.getAuthor(), duration, substrings.get(0), choicesList);
// }
//
// @Override
// public EmbedObject getHelp(String prefix) {
// return new HelpBuilder(this, prefix)
// .setDescription("Create a poll.")
// .addArg("duration", String.format("in seconds, must be between %ds and %ds (1 hour)", MIN_DURATION, MAX_DURATION), false)
// .addArg("\"question\"", false)
// .addArg("choice1", false)
// .addArg("choice2", false)
// .addArg("choiceX", true)
// .setExample(String.format("`%s%s 120 \"Where do we eat at noon?\" \"White\" \"53\" \"A dog\"`", prefix, this.getName()))
// .addField("Restrictions", String.format("**question and choices** - must be in quotation marks"
// + "%n**choices** - min: %d, max: %d", MIN_CHOICES_NUM, MAX_CHOICES_NUM), false)
// .build();
// }
// }
