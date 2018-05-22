// package me.shadorc.shadbot.command.game.hangman;
//
// import java.io.IOException;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.concurrent.ConcurrentHashMap;
// import java.util.concurrent.ThreadLocalRandom;
// import java.util.stream.Collectors;
//
// import org.json.JSONException;
//
// import me.shadorc.shadbot.core.command.AbstractCommand;
// import me.shadorc.shadbot.core.command.CommandCategory;
// import me.shadorc.shadbot.core.command.Context;
// import me.shadorc.shadbot.core.command.annotation.Command;
// import me.shadorc.shadbot.core.command.annotation.RateLimited;
// import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
// import me.shadorc.shadbot.exception.MissingArgumentException;
// import me.shadorc.shadbot.utils.BotUtils;
// import me.shadorc.shadbot.utils.FormatUtils;
// import me.shadorc.shadbot.utils.NetUtils;
// import me.shadorc.shadbot.utils.StringUtils;
// import me.shadorc.shadbot.utils.Utils;
// import me.shadorc.shadbot.utils.embed.HelpBuilder;
// import me.shadorc.shadbot.utils.object.Emoji;
// import me.shadorc.shadbot.utils.object.message.LoadingMessage;
//
// @RateLimited
// @Command(category = CommandCategory.GAME, names = { "hangman" })
// public class HangmanCmd extends AbstractCommand {
//
// protected enum Difficulty {
// EASY, HARD;
// }
//
// private static final int MIN_WORD_LENGTH = 5;
// private static final int MAX_WORD_LENGTH = 10;
//
// protected static final ConcurrentHashMap<Long, HangmanManager> MANAGERS = new ConcurrentHashMap<>();
// protected static final List<String> HARD_WORDS = new ArrayList<>();
// protected static final List<String> EASY_WORDS = new ArrayList<>();
//
// @Override
// public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
// Difficulty difficulty = Utils.getValueOrNull(Difficulty.class, context.getArg());
//
// if(context.hasArg() && difficulty == null) {
// throw new IllegalCmdArgumentException(String.format("`%s` is not a valid difficulty. %s",
// context.getArg(), FormatUtils.formatOptions(Difficulty.class)));
// }
//
// if(difficulty == null) {
// difficulty = Difficulty.EASY;
// }
//
// if(HARD_WORDS.isEmpty() || EASY_WORDS.isEmpty()) {
// LoadingMessage loadingMsg = new LoadingMessage("Loading word...", context.getChannel());
// loadingMsg.send();
// try {
// this.load();
// } catch (JSONException | IOException err) {
// Utils.handle("getting words list", context, err);
// }
// loadingMsg.delete();
// }
//
// HangmanManager hangmanManager = MANAGERS.get(context.getChannel().getLongID());
//
// if(hangmanManager == null) {
// hangmanManager = new HangmanManager(this, context.getPrefix(), context.getChannel(), context.getAuthor(), difficulty);
// if(MANAGERS.putIfAbsent(context.getChannel().getLongID(), hangmanManager) == null) {
// hangmanManager.start();
// }
// } else {
// BotUtils.sendMessage(String.format(Emoji.INFO + " A Hangman game has already been started by **%s**. Please, wait for him to finish.",
// hangmanManager.getAuthor().getName()), context.getChannel());
// }
// }
//
// private void load() throws JSONException, IOException {
// if(HARD_WORDS.isEmpty()) {
// String url = "https://raw.githubusercontent.com/dwyl/english-words/master/words_alpha.txt";
// HARD_WORDS.addAll(StringUtils.split(NetUtils.getBody(url), "\n").stream()
// .filter(word -> Utils.isInRange(word.length(), MIN_WORD_LENGTH, MAX_WORD_LENGTH))
// .limit(500)
// .collect(Collectors.toList()));
// }
//
// if(EASY_WORDS.isEmpty()) {
// String url = "https://gist.githubusercontent.com/deekayen/4148741/raw/01c6252ccc5b5fb307c1bb899c95989a8a284616/1-1000.txt";
// EASY_WORDS.addAll(StringUtils.split(NetUtils.getBody(url), "\n").stream()
// .filter(word -> Utils.isInRange(word.length(), MIN_WORD_LENGTH, MAX_WORD_LENGTH))
// .limit(500)
// .collect(Collectors.toList()));
// }
// }
//
// protected static String getWord(Difficulty difficulty) {
// if(difficulty.equals(Difficulty.EASY)) {
// return EASY_WORDS.get(ThreadLocalRandom.current().nextInt(HangmanCmd.EASY_WORDS.size()));
// }
// return HARD_WORDS.get(ThreadLocalRandom.current().nextInt(HangmanCmd.HARD_WORDS.size()));
// }
//
// @Override
// public EmbedObject getHelp(String prefix) {
// return new HelpBuilder(this, prefix)
// .setDescription("Start a Hangman game.")
// .addArg("difficulty", String.format("%s. The difficulty of the word to find",
// FormatUtils.format(Difficulty.values(), value -> value.toString().toLowerCase(), "/")), true)
// .setGains("The winner gets **%d coins** plus a bonus depending on the number of errors.", HangmanManager.MIN_GAINS)
// .build();
// }
// }
