package me.shadorc.discordbot.command.game.blackjack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.swing.Timer;

import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.StatCategory;
import me.shadorc.discordbot.data.Stats;
import me.shadorc.discordbot.data.Storage;
import me.shadorc.discordbot.message.MessageListener;
import me.shadorc.discordbot.message.MessageManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.game.Card;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;

public class BlackjackManager implements MessageListener {

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

	public void startIfNecessary() {
		if(!dealerCards.isEmpty()) {
			return;
		}

		dealerCards.addAll(BlackjackUtils.pickCards(2));
		while(BlackjackUtils.getValue(dealerCards) <= 16) {
			dealerCards.addAll(BlackjackUtils.pickCards(1));
		}

		MessageManager.addListener(context.getChannel(), this);
		timer.start();
		startTime = System.currentTimeMillis();

		this.stopOrShow();
	}

	public void stop() {
		timer.stop();

		MessageManager.removeListener(context.getChannel());
		BlackjackCmd.CHANNELS_BLACKJACK.remove(context.getChannel().getLongID());

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
		if(message != null && BotUtils.hasPermission(context.getChannel(), Permissions.MANAGE_MESSAGES)) {
			message.delete();
		}

		EmbedBuilder builder = Utils.getDefaultEmbed()
				.withAuthorName("Blackjack")
				.withThumbnail("https://pbs.twimg.com/profile_images/1874281601/BlackjackIcon_400x400.png")
				.appendDescription("**Use `" + context.getPrefix() + "blackjack <bet>` to join the game.**"
						+ "\n\nType `hit` to take another card, `stand` to pass or `double down` to double down.")
				.appendField("Dealer's hand", BlackjackUtils.formatCards(isFinished ? dealerCards : dealerCards.subList(0, 1)), true)
				.withFooterText(isFinished ? "Finished" : "This game will end automatically in " + StringUtils.formatDuration(timer.getDelay() - System.currentTimeMillis() + startTime));

		for(BlackjackPlayer player : players) {
			builder.appendField(player.getUser().getName() + "'s hand" + (player.hasDoubleDown() ? " (Double down)" : ""),
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

			int result = -1; // 0 = Win | 1 = Draw | 2 = Lose
			if(playerValue > 21) {
				result = 2;
			} else if(dealerValue <= 21) {
				if(playerValue > dealerValue) {
					result = 0;
				} else if(playerValue == dealerValue) {
					result = 1;
				} else if(playerValue < dealerValue) {
					result = 2;
				}
			} else {
				result = 0;
			}

			StringBuilder strBuilder = new StringBuilder("**" + player.getUser().getName() + "** ");
			switch (result) {
				case 0:
					strBuilder.append("(Gains: " + StringUtils.pluralOf(player.getBet(), "coin") + ")");
					Storage.addCoins(context.getGuild(), player.getUser(), player.getBet());
					Stats.increment(StatCategory.MONEY_GAINS_COMMAND, "blackjack", player.getBet());
					break;
				case 1:
					strBuilder.append("(Draw)");
					break;
				case 2:
					strBuilder.append("(Losses: " + StringUtils.pluralOf(player.getBet(), "coin") + ")");
					Storage.addCoins(context.getGuild(), player.getUser(), -player.getBet());
					Stats.increment(StatCategory.MONEY_LOSSES_COMMAND, "blackjack", player.getBet());
					break;
			}
			results.add(strBuilder.toString());
		}

		BotUtils.sendMessage(Emoji.DICE + " __Results:__ " + StringUtils.formatList(results, str -> str, ", "), context.getChannel());
	}

	@Override
	public boolean onMessageReceived(IMessage message) {
		List<BlackjackPlayer> matchingPlayers = players.stream().filter(playerItr -> playerItr.getUser().equals(message.getAuthor())).collect(Collectors.toList());
		if(matchingPlayers.isEmpty()) {
			return false;
		}

		BlackjackPlayer player = matchingPlayers.get(0);

		switch (message.getContent().trim()) {
			case "hit":
				player.hit();
				this.stopOrShow();
				break;
			case "stand":
				player.stand();
				this.stopOrShow();
				break;
			case "double down":
				if(player.getCards().size() != 2) {
					BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " " + player.getUser().getName()
							+ ", you must have a maximum of 2 cards to use `double down`.", context.getChannel());
					return false;
				}
				player.doubleDown();
				this.stopOrShow();
				break;
		}

		return false;
	}
}