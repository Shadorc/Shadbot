package me.shadorc.shadbot.command.game.dice;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.game.AbstractGameManager;
import me.shadorc.shadbot.data.db.DatabaseManager;
import me.shadorc.shadbot.data.stats.MoneyStatsManager;
import me.shadorc.shadbot.data.stats.MoneyStatsManager.MoneyEnum;
import me.shadorc.shadbot.listener.interceptor.MessageInterceptor;
import me.shadorc.shadbot.listener.interceptor.MessageInterceptorManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.message.UpdateableMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class DiceManager extends AbstractGameManager implements MessageInterceptor {

	private static final int GAME_DURATION = 30;

	private final ConcurrentHashMap<Integer, Snowflake> numsPlayers;
	private final int bet;
	private final UpdateableMessage updateableMessage;

	private String results;

	public DiceManager(Context context, int bet) {
		super(context);
		this.bet = bet;
		this.numsPlayers = new ConcurrentHashMap<>();
		this.updateableMessage = new UpdateableMessage(context.getClient(), context.getChannelId());
	}

	@Override
	public Mono<Void> start() {
		return Mono.fromRunnable(() -> {
			this.schedule(this.rollTheDice(), GAME_DURATION, ChronoUnit.SECONDS);
			MessageInterceptorManager.addInterceptor(this.getContext().getChannelId(), this);
		});
	}

	@Override
	public Mono<Void> stop() {
		return Mono.fromRunnable(() -> {
			this.cancelScheduledTask();
			MessageInterceptorManager.removeInterceptor(this.getContext().getChannelId(), this);
			DiceCmd.MANAGERS.remove(this.getContext().getChannelId());
		});
	}

	public Mono<Void> rollTheDice() {
		int winningNum = ThreadLocalRandom.current().nextInt(1, 7);
		return Flux.fromIterable(numsPlayers.values())
				.flatMap(this.getContext().getClient()::getUserById)
				.map(user -> {
					int num = numsPlayers.keySet().stream()
							.filter(number -> numsPlayers.get(number).equals(user.getId()))
							.findFirst()
							.get();
					int gains = bet;

					if(num == winningNum) {
						gains *= numsPlayers.size() + DiceCmd.MULTIPLIER;
						MoneyStatsManager.log(MoneyEnum.MONEY_GAINED, this.getContext().getCommandName(), gains);
					} else {
						gains *= -1;
						MoneyStatsManager.log(MoneyEnum.MONEY_LOST, this.getContext().getCommandName(), Math.abs(gains));
					}
					DatabaseManager.getDBMember(this.getContext().getGuildId(), user.getId()).addCoins(gains);
					return String.format("%s (**%s**)", user.getUsername(), FormatUtils.formatCoins(gains));
				})
				.collectList()
				.map(list -> this.results = String.join("\n", list))
				.then(BotUtils.sendMessage(String.format(Emoji.DICE + " The dice is rolling... **%s** !", winningNum), this.getContext().getChannel()))
				.then(this.show())
				.then(this.stop());
	}

	protected Mono<Message> show() {
		return this.getContext().getAvatarUrl()
				.zipWith(Flux.fromIterable(numsPlayers.values())
						.flatMap(this.getContext().getClient()::getUserById)
						.map(User::getUsername)
						.collectList())
				.map(avatarUrlAndUsers -> {
					final String avatarUrl = avatarUrlAndUsers.getT1();
					final List<String> usernames = avatarUrlAndUsers.getT2();
					EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed()
							.setAuthor("Dice Game", null, avatarUrl)
							.setThumbnail("http://findicons.com/files/icons/2118/nuvola/128/package_games_board.png")
							.setDescription(String.format("**Use `%s%s <num>` to join the game.**%n**Bet:** %s",
									this.getContext().getPrefix(), this.getContext().getCommandName(), FormatUtils.formatCoins(bet)))
							.addField("Player", String.join("\n", usernames), true)
							.addField("Number", FormatUtils.format(numsPlayers.keySet(), Object::toString, "\n"), true);

					if(this.isTaskDone()) {
						embed.setFooter("Finished.", null);
					} else {
						embed.setFooter(String.format("You have %d seconds to make your bets.", GAME_DURATION), null);
					}

					if(results != null) {
						embed.addField("Results", results, false);
					}

					return embed;
				})
				.flatMap(updateableMessage::send);
	}

	public int getBet() {
		return bet;
	}

	public int getPlayersCount() {
		return numsPlayers.size();
	}

	/**
	 * @param userId - the used ID to add
	 * @param num - the number bet by the user
	 * @return true if the user could be added, false otherwise
	 */
	public boolean addPlayer(Snowflake userId, int num) {
		if(numsPlayers.containsValue(userId)) {
			return false;
		}

		numsPlayers.putIfAbsent(num, userId);
		return true;
	}

	protected boolean isNumBet(int num) {
		return numsPlayers.containsKey(num);
	}

	@Override
	public Mono<Boolean> isIntercepted(MessageCreateEvent event) {
		return this.cancelOrDo(event.getMessage(), Mono.empty());
	}
}
