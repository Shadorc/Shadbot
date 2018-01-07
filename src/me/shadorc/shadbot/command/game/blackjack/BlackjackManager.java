package me.shadorc.shadbot.command.game.blackjack;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.game.AbstractGameManager;
import me.shadorc.shadbot.message.MessageListener;
import me.shadorc.shadbot.message.MessageManager;
import me.shadorc.shadbot.ratelimiter.RateLimiter;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DateUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.Card;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.UpdateableMessage;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class BlackjackManager extends AbstractGameManager implements MessageListener {

	private static final int GAME_DURATION = 60;
	private static final float WIN_MULTIPLIER = 1.035f;

	private final RateLimiter rateLimiter;
	private final List<BlackjackPlayer> players;
	private final List<Card> dealerCards;
	private final UpdateableMessage message;

	private long startTime;

	public BlackjackManager(AbstractCommand cmd, IChannel channel, IUser author) {
		super(cmd, channel, author);
		this.rateLimiter = new RateLimiter(1, 2, ChronoUnit.SECONDS);
		this.players = Collections.synchronizedList(new ArrayList<>());
		this.dealerCards = new ArrayList<>();
		this.message = new UpdateableMessage(channel);
	}

	@Override
	public void start() {
		this.dealerCards.addAll(Card.pick(2));
		while(BlackjackUtils.getValue(this.dealerCards) < 17) {
			this.dealerCards.add(Card.pick());
		}

		MessageManager.addListener(this.getChannel(), this);
		this.schedule(() -> this.stop(), GAME_DURATION, TimeUnit.SECONDS);
		startTime = System.currentTimeMillis();
	}

	@Override
	public void stop() {
		this.cancelScheduledTask();
		MessageManager.removeListener(this.getChannel(), this);
		BlackjackCmd.MANAGERS.remove(this.getChannel().getLongID());

		this.show();
		this.computeResults();

		dealerCards.clear();
		players.clear();
	}

	public boolean addPlayerIfAbsent(IUser user, int bet) {
		if(players.stream().anyMatch(player -> player.getUser().equals(user))) {
			return false;
		}
		players.add(new BlackjackPlayer(user, bet));
		this.stopOrShow();
		return true;
	}

	private void stopOrShow() {
		if(players.stream().noneMatch(BlackjackPlayer::isStanding)) {
			this.show();
		} else {
			this.stop();
		}
	}

	private void show() {
		EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
				.withAuthorName("Blackjack")
				.withThumbnail("https://pbs.twimg.com/profile_images/1874281601/BlackjackIcon_400x400.png")
				.appendDescription(String.format("**Use `%s%s <bet>` to join the game.**"
						+ "%n%nType `hit` to take another card, `stand` to pass or `double down` to double down.",
						this.getPrefix(), this.getCmdName()))
				.appendField("Dealer's hand", BlackjackUtils.formatCards(this.isTaskDone() ? dealerCards : dealerCards.subList(0, 1)), true);

		if(this.isTaskDone()) {
			embed.withFooterText("Finished");
		} else {
			long remainingTime = GAME_DURATION - TimeUnit.MILLISECONDS.toSeconds(DateUtils.getMillisUntil(startTime));
			embed.withFooterText(String.format("This game will end automatically in %d seconds.", remainingTime));
		}

		players.stream().forEach(player -> embed.appendField(
				String.format("%s's hand%s%s",
						player.getUser().getName(), player.isStanding() ? " (Stand)" : "", player.isDoubleDown() ? " (Double down)" : ""),
				BlackjackUtils.formatCards(player.getCards()),
				true));

		message.send(embed.build()).get();
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

			int gains = player.getBet();
			String username = player.getUser().getName();
			String gainsStr = FormatUtils.formatCoins(gains);
			switch (result) {
				case 1:
					gains = (int) Math.ceil(gains * WIN_MULTIPLIER);
					results.add(0, String.format("**%s** (Gains: **%s**)", username, gainsStr));
					break;
				case 0:
					gains *= 0;
					results.add(String.format("**%s** (Draw)", username));
					break;
				case -1:
					gains *= -1;
					results.add(results.size(), String.format("**%s** (Losses: **%s**)", username, gainsStr));
					break;
			}

			Database.getDBUser(this.getGuild(), player.getUser()).addCoins(gains);
			// StatsManager.increment(CommandManager.getFirstName(context.getCommand()), gains);
		}

		BotUtils.sendMessage(Emoji.DICE + " __Results:__ " + FormatUtils.format(results, str -> str, ", "), this.getChannel());
	}

	@Override
	public boolean intercept(IMessage message) {
		BlackjackPlayer player = players.stream().filter(playerItr -> playerItr.getUser().equals(message.getAuthor())).findAny().get();
		if(player == null) {
			return false;
		}

		if(rateLimiter.isLimited(message.getChannel(), message.getAuthor())) {
			return true;
		}

		if(player.isStanding()) {
			BotUtils.sendMessage(String.format(Emoji.GREY_EXCLAMATION + " (**%s**) You're standing, you can't play anymore.",
					this.getAuthor().getName()), message.getChannel());
			return false;
		}

		String content = message.getContent().toLowerCase().trim();

		if("double down".equals(content) && player.getCards().size() != 2) {
			BotUtils.sendMessage(String.format(Emoji.GREY_EXCLAMATION + " (**%s**) You must have a maximum of 2 cards to use `double down`.",
					player.getUser().getName()), message.getChannel());
			return true;
		}

		Map<String, Runnable> actionsMap = new HashMap<>();
		actionsMap.put("hit", () -> player.hit());
		actionsMap.put("stand", () -> player.stand());
		actionsMap.put("double down", () -> player.doubleDown());

		Runnable action = actionsMap.get(content);
		if(action == null) {
			return false;
		}

		action.run();
		this.stopOrShow();
		return true;
	}

}