// TODO
// package me.shadorc.shadbot.command.game.blackjack;
//
// import java.util.concurrent.ConcurrentHashMap;
//
// import me.shadorc.shadbot.core.command.AbstractCommand;
// import me.shadorc.shadbot.core.command.CommandCategory;
// import me.shadorc.shadbot.core.command.Context;
// import me.shadorc.shadbot.core.command.annotation.Command;
// import me.shadorc.shadbot.core.command.annotation.RateLimited;
// import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
// import me.shadorc.shadbot.exception.MissingArgumentException;
// import me.shadorc.shadbot.utils.BotUtils;
// import me.shadorc.shadbot.utils.Utils;
// import me.shadorc.shadbot.utils.embed.HelpBuilder;
// import me.shadorc.shadbot.utils.object.Emoji;
//
// @RateLimited
// @Command(category = CommandCategory.GAME, names = { "blackjack" }, alias = "bj")
// public class BlackjackCmd extends AbstractCommand {
//
// protected static final ConcurrentHashMap<Long, BlackjackManager> MANAGERS = new ConcurrentHashMap<>();
//
// private static final int MAX_BET = 250_000;
//
// @Override
// public void execute(Context context) {
// if(!context.hasArg()) {
// throw new MissingArgumentException();
// }
//
// Integer bet = Utils.checkAndGetBet(context.getChannel(), context.getAuthor(), context.getArg(), MAX_BET);
// if(bet == null) {
// return;
// }
//
// BlackjackManager blackjackManager = MANAGERS.get(context.getChannel().getLongID());
// if(blackjackManager == null) {
// blackjackManager = new BlackjackManager(this, context.getPrefix(), context.getChannel(), context.getAuthor());
// }
//
// if(MANAGERS.putIfAbsent(context.getChannel().getLongID(), blackjackManager) == null) {
// blackjackManager.start();
// }
//
// if(!blackjackManager.addPlayerIfAbsent(context.getAuthor(), bet)) {
// BotUtils.sendMessage(String.format(Emoji.INFO + " (**%s**) You're already participating.",
// context.getUsername()), context.getChannel());
// }
// }
//
// @Override
// public EmbedObject getHelp(String prefix) {
// return new HelpBuilder(this, context)
// .setDescription("Start or join a blackjack game.")
// .addArg("bet", false)
// .addField("Info", "**double down** - increase the initial bet by 100% in exchange for committing to stand"
// + " after receiving exactly one more card", false)
// .build();
// }
// }
