package me.shadorc.discordbot.command.game.blackjack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.swing.Timer;

import me.shadorc.discordbot.command.CommandManager;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.data.LottoDataManager;
import me.shadorc.discordbot.data.StatCategory;
import me.shadorc.discordbot.data.StatsManager;
import me.shadorc.discordbot.message.MessageListener;
import me.shadorc.discordbot.message.MessageManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.FormatUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.game.Card;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class BlackjackManager implements MessageListener {

	protected static final ConcurrentHashMap<Long, BlackjackManager> CHANNELS_BLACKJACK = new ConcurrentHashMap<>();

	private static final int GAME_DURATION = 60;

	private final List<BlackjackPlayer> players;
	private final List<Card> dealerCards;
	private final Context context;
	private final Timer timer;

	private long startTime;
	private IMessage message;

	public BlackjackManager(Context context) {
		this.players = Collections.synchronizedList(new ArrayList<>());
		this.dealerCards = new ArrayList<>();
		this.context = context;
		this.timer = new Timer((int) TimeUnit.SECONDS.toMillis(GAME_DURATION), event -> {
			this.stop();
		});
	}

	public void start() {
		this.dealerCards.addAll(BlackjackUtils.pickCards(2));
		while(BlackjackUtils.getValue(this.dealerCards) <= 16) {
			this.dealerCards.addAll(BlackjackUtils.pickCards(1));
		}

		MessageManager.addListener(context.getChannel(), this);
		timer.start();
		startTime = System.currentTimeMillis();
		CHANNELS_BLACKJACK.putIfAbsent(context.getChannel().getLongID(), this);
	}

	public void stop() {
		timer.stop();

		MessageManager.removeListener(context.getChannel(), this);
		CHANNELS_BLACKJACK.remove(context.getChannel().getLongID());

		this.show(true);
		this.computeResults();

		dealerCards.clear();
		players.clear();
	}

	public void addPlayer(IUser user, int bet) {
		BlackjackPlayer player = new BlackjackPlayer(user, bet);
		player.addCards(BlackjackUtils.pickCards(2));
		players.add(player);
		this.stopOrShow();
	}

	public boolean isPlaying(IUser user) {
		return players.stream().anyMatch(player -> player.getUser().equals(user));
	}

	private void show(boolean isFinished) {
		BotUtils.deleteIfPossible(context.getChannel(), message);

		EmbedBuilder builder = Utils.getDefaultEmbed()
				.withAuthorName("Blackjack")
				.withThumbnail("https://pbs.twimg.com/profile_images/1874281601/BlackjackIcon_400x400.png")
				.appendDescription("**Use `" + context.getPrefix() + CommandManager.getFirstName(context.getCommand())
						+ " <bet>` to join the game.**"
						+ "\n\nType `hit` to take another card, `stand` to pass or `double down` to double down.")
				.appendField("Dealer's hand", BlackjackUtils.formatCards(isFinished ? dealerCards : dealerCards.subList(0, 1)), true)
				.withFooterText(isFinished ? "Finished" : "This game will end automatically in " + FormatUtils.formatDuration(timer.getDelay() - System.currentTimeMillis() + startTime));

		for(BlackjackPlayer player : players) {
			builder.appendField(player.getUser().getName() + "'s hand"
					+ (player.isStanding() ? " (Stand)" : "")
					+ (player.hasDoubleDown() ? " (Double down)" : ""),
					BlackjackUtils.formatCards(player.getCards()), true);
		}

		message = BotUtils.sendMessage(builder.build(), context.getChannel()).get();
	}

	private void stopOrShow() {
		if(players.stream().anyMatch(playerItr -> !playerItr.isStanding())) {
			this.show(false);
		} else {
			this.stop();
		}
	}

	private void computeResults() {
		int dealerValue = BlackjackUtils.getValue(dealerCards);

		List<String> results = new ArrayList<>();
		for(BlackjackPlayer player : players) {
			int playerValue = BlackjackUtils.getValue(player.getCards());

			int result; // -1 = Lose | 0 = Draw | 1 = Win
			if(playerValue > 21) {
				result = -1;
			} else if(dealerValue <= 21) {
				result = Integer.valueOf(playerValue).compareTo(dealerValue);
			} else {
				result = 1;
			}

			StringBuilder strBuilder = new StringBuilder("**" + player.getUser().getName() + "** ");
			switch (result) {
				case -1:
					strBuilder.append("(Losses: *" + FormatUtils.formatCoins(player.getBet()) + "*)");
					StatsManager.increment(StatCategory.MONEY_LOSSES_COMMAND, CommandManager.getFirstName(context.getCommand()), player.getBet());
					LottoDataManager.addToPool(player.getBet());
					break;
				case 0:
					strBuilder.append("(Draw)");
					break;
				case 1:
					strBuilder.append("(Gains: *" + FormatUtils.formatCoins(player.getBet()) + "*)");
					StatsManager.increment(StatCategory.MONEY_GAINS_COMMAND, CommandManager.getFirstName(context.getCommand()), player.getBet());
					break;
			}

			DatabaseManager.addCoins(context.getChannel(), player.getUser(), result * player.getBet());
			results.add(strBuilder.toString());
		}

		BotUtils.sendMessage(Emoji.DICE + " __Results:__ " + FormatUtils.formatList(results, str -> str, ", "), context.getChannel());
	}

	@Override
	public boolean onMessageReceived(IMessage message) {
		List<BlackjackPlayer> matchingPlayers = players.stream().filter(playerItr -> playerItr.getUser().equals(message.getAuthor())).collect(Collectors.toList());
		if(matchingPlayers.isEmpty()) {
			return false;
		}

		BlackjackPlayer player = matchingPlayers.get(0);

		if(player.isStanding()) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " (**" + context.getAuthorName() + "**) You're standing, you can't play anymore.", context.getChannel());
			return false;
		}

		if(message.getContent().trim().equals("double down") && player.getCards().size() != 2) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " (**" + player.getUser().getName()
					+ "**) You must have a maximum of 2 cards to use `double down`.", context.getChannel());
			return true;
		}

		Map<String, Runnable> actionsMap = new HashMap<>();
		actionsMap.put("hit", () -> player.hit());
		actionsMap.put("stand", () -> player.stand());
		actionsMap.put("double down", () -> player.doubleDown());

		Runnable action = actionsMap.get(message.getContent().trim());
		if(action != null) {
			action.run();
			this.stopOrShow();
			return true;
		}

		return false;
	}
}