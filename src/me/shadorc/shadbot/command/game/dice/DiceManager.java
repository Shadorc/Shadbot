package me.shadorc.shadbot.command.game.dice;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.game.AbstractGameManager;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.MoneyEnum;
import me.shadorc.shadbot.listener.interceptor.MessageInterceptor;
import me.shadorc.shadbot.listener.interceptor.MessageInterceptorManager;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.UpdateableMessage;
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
	public void start() {
		this.schedule(this.rollTheDice(), GAME_DURATION, ChronoUnit.SECONDS);
		MessageInterceptorManager.addInterceptor(this.getContext().getChannelId(), this);
	}

	@Override
	public void stop() {
		this.cancelScheduledTask();
		MessageInterceptorManager.removeInterceptor(this.getContext().getChannelId(), this);
		DiceCmd.MANAGERS.remove(this.getContext().getChannelId());
	}

	@Override
	public Mono<Void> show() {
		return Flux.fromIterable(this.numsPlayers.values())
				.flatMap(this.getContext().getClient()::getUserById)
				.map(User::getUsername)
				.collectList()
				.map(usernames -> EmbedUtils.getDefaultEmbed()
						.andThen(embed -> {
						embed.setAuthor("Dice Game", null, this.getContext().getAvatarUrl())
							.setThumbnail("http://findicons.com/files/icons/2118/nuvola/128/package_games_board.png")
							.setDescription(String.format("**Use `%s%s <num>` to join the game.**%n**Bet:** %s",
									this.getContext().getPrefix(), this.getContext().getCommandName(), FormatUtils.coins(this.bet)))
							.addField("Player", String.join("\n", usernames), true)
							.addField("Number", FormatUtils.format(this.numsPlayers.keySet(), Object::toString, "\n"), true);
						
						if(this.isTaskDone()) {
							embed.setFooter("Finished.", null);
						} else {
							embed.setFooter(String.format("You have %d seconds to make your bets.", GAME_DURATION), null);
						}
						
						if(this.results != null) {
							embed.addField("Results", this.results, false);
						}
					}))
				.flatMap(this.updateableMessage::send)
				.then();
	}

	public Mono<Void> rollTheDice() {
		final int winningNum = ThreadLocalRandom.current().nextInt(1, 7);
		return Flux.fromIterable(this.numsPlayers.values())
				.flatMap(this.getContext().getClient()::getUserById)
				.map(user -> {
					final int num = this.numsPlayers.keySet().stream()
							.filter(number -> this.numsPlayers.get(number).equals(user.getId()))
							.findFirst()
							.get();
					int gains = this.bet;

					if(num == winningNum) {
						gains *= this.numsPlayers.size() + DiceCmd.MULTIPLIER;
						StatsManager.MONEY_STATS.log(MoneyEnum.MONEY_GAINED, this.getContext().getCommandName(), gains);
					} else {
						gains *= -1;
						StatsManager.MONEY_STATS.log(MoneyEnum.MONEY_LOST, this.getContext().getCommandName(), Math.abs(gains));
						Shadbot.getLottery().addToJackpot(Math.abs(gains));
					}
					Shadbot.getDatabase().getDBMember(this.getContext().getGuildId(), user.getId()).addCoins(gains);
					return String.format("%s (**%s**)", user.getUsername(), FormatUtils.coins(gains));
				})
				.collectList()
				.map(list -> this.results = String.join("\n", list))
				.then(this.getContext().getChannel())
				.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.DICE + " The dice is rolling... **%s** !", winningNum), channel))
				.then(this.show())
				.then(Mono.fromRunnable(this::stop));
	}

	public int getBet() {
		return this.bet;
	}

	public int getPlayerCount() {
		return this.numsPlayers.size();
	}

	public boolean addPlayerIfAbsent(Snowflake userId, int num) {
		if(this.numsPlayers.containsValue(userId)) {
			return false;
		}

		this.numsPlayers.putIfAbsent(num, userId);
		return true;
	}

	protected boolean isNumBet(int num) {
		return this.numsPlayers.containsKey(num);
	}

	@Override
	public Mono<Boolean> isIntercepted(MessageCreateEvent event) {
		return this.cancelOrDo(event.getMessage(), Mono.empty());
	}
}
