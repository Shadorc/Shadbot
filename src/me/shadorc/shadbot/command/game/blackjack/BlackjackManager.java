package me.shadorc.shadbot.command.game.blackjack;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import discord4j.common.json.EmbedFieldEntity;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.game.AbstractGameManager;
import me.shadorc.shadbot.core.ratelimiter.RateLimiter;
import me.shadorc.shadbot.data.db.DatabaseManager;
import me.shadorc.shadbot.data.stats.MoneyStatsManager;
import me.shadorc.shadbot.data.stats.MoneyStatsManager.MoneyEnum;
import me.shadorc.shadbot.listener.interceptor.MessageInterceptor;
import me.shadorc.shadbot.listener.interceptor.MessageInterceptorManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.command.Card;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.message.UpdateableMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class BlackjackManager extends AbstractGameManager implements MessageInterceptor {

	private static final int GAME_DURATION = 60;
	private static final float WIN_MULTIPLIER = 1.15f;

	private final RateLimiter rateLimiter;
	private final List<BlackjackPlayer> players;
	private final List<Card> dealerCards;
	private final UpdateableMessage updateableMessage;

	private long startTime;

	public BlackjackManager(Context context) {
		super(context);
		this.rateLimiter = new RateLimiter(1, 2, ChronoUnit.SECONDS);
		this.players = new CopyOnWriteArrayList<>();
		this.dealerCards = new ArrayList<>();
		this.updateableMessage = new UpdateableMessage(context.getClient(), context.getChannelId());
	}

	@Override
	public Mono<Void> start() {
		this.dealerCards.addAll(Card.pick(2));
		while(BlackjackUtils.getValue(this.dealerCards) < 17) {
			this.dealerCards.add(Card.pick());
		}

		this.schedule(this.stop(), GAME_DURATION, ChronoUnit.SECONDS);
		MessageInterceptorManager.addInterceptor(this.getContext().getChannelId(), this);
		this.startTime = System.currentTimeMillis();
		return Mono.empty();
	}

	@Override
	public Mono<Void> stop() {
		this.cancelScheduledTask();
		MessageInterceptorManager.removeInterceptor(this.getContext().getChannelId(), this);
		// BlackjackCmd.MANAGERS.remove(this.getContext().getChannelId());

		return this.show()
				.then(this.computeResults());
	}

	public boolean addPlayerIfAbsent(Snowflake userId, int bet) {
		if(players.stream().map(BlackjackPlayer::getUserId).anyMatch(userId::equals)) {
			return false;
		}
		return players.add(new BlackjackPlayer(userId, bet));
	}

	public Mono<Void> stopOrShow() {
		return players.stream().allMatch(BlackjackPlayer::isStanding) ? this.stop() : this.show().then();
	}

	private Mono<Message> show() {
		return Flux.fromIterable(players)
				.flatMap(player -> this.getContext().getClient().getUserById(player.getUserId()))
				.map(user -> {
					BlackjackPlayer player = players.stream().filter(playerItr -> playerItr.getUserId().equals(user.getId())).findFirst().get();
					return new EmbedFieldEntity(String.format("%s's hand%s%s",
							user.getUsername(), player.isStanding() ? " (Stand)" : "", player.isDoubleDown() ? " (Double down)" : ""),
							BlackjackUtils.formatCards(player.getCards()), true);
				})
				.collectList()
				.zipWith(this.getContext().getAvatarUrl())
				.map(fieldsAndAvatarUrl -> {
					final List<EmbedFieldEntity> fields = fieldsAndAvatarUrl.getT1();
					final String avatarUrl = fieldsAndAvatarUrl.getT2();

					EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed()
							.setAuthor("Blackjack", null, avatarUrl)
							.setThumbnail("https://pbs.twimg.com/profile_images/1874281601/BlackjackIcon_400x400.png")
							.setDescription(String.format("**Use `%s%s <bet>` to join the game.**"
									+ "%n%nType `hit` to take another card, `stand` to pass or `double down` to double down.",
									this.getContext().getPrefix(), this.getContext().getCommandName()))
							.addField("Dealer's hand", BlackjackUtils.formatCards(this.isTaskDone() ? dealerCards : dealerCards.subList(0, 1)), true);

					if(this.isTaskDone()) {
						embed.setFooter("Finished", null);
					} else {
						long remainingTime = GAME_DURATION - TimeUnit.MILLISECONDS.toSeconds(TimeUtils.getMillisUntil(startTime));
						embed.setFooter(String.format("This game will end automatically in %d seconds.", remainingTime), null);
					}

					fields.stream()
							.forEach(field -> embed.addField(field.getName(), field.getValue(), field.isInline()));

					return embed;
				})
				.flatMap(updateableMessage::send);
	}

	private Mono<Void> computeResults() {
		int dealerValue = BlackjackUtils.getValue(dealerCards);

		return Flux.fromIterable(players)
				.map(BlackjackPlayer::getUserId)
				.flatMap(userId -> this.getContext().getClient().getUserById(userId))
				.map(user -> {
					final BlackjackPlayer player = players.stream().filter(playerItr -> playerItr.getUserId().equals(user.getId())).findFirst().get();
					final int playerValue = BlackjackUtils.getValue(player.getCards());

					int result; // -1 = Lose | 0 = Draw | 1 = Win
					if(playerValue > 21) {
						result = -1;
					} else if(dealerValue <= 21) {
						result = Integer.valueOf(playerValue).compareTo(dealerValue);
					} else {
						result = 1;
					}

					int gains = 0;
					String text = new String();
					switch (result) {
						case 1:
							gains = (int) Math.ceil(player.getBet() * WIN_MULTIPLIER);
							MoneyStatsManager.log(MoneyEnum.MONEY_GAINED, this.getContext().getCommandName(), gains);
							text = String.format("**%s** (Gains: **%s**)", user.getUsername(), FormatUtils.formatCoins(gains));
							break;
						case 0:
							gains = 0;
							text = String.format("**%s** (Draw)", user.getUsername());
						case -1:
							gains = -player.getBet();
							MoneyStatsManager.log(MoneyEnum.MONEY_LOST, this.getContext().getCommandName(), Math.abs(gains));
							text = String.format("**%s** (Losses: **%s**)", user.getUsername(), FormatUtils.formatCoins(Math.abs(gains)));
					}

					DatabaseManager.getDBMember(this.getContext().getGuildId(), user.getId()).addCoins(gains);
					return text;
				})
				.collectList()
				.flatMap(results -> BotUtils.sendMessage(
						String.format(Emoji.DICE + " __Results:__ %s", String.join(", ", results)), this.getContext().getChannel()))
				.then();
	}

	@Override
	public Mono<Boolean> isIntercepted(MessageCreateEvent event) {
		final Member member = event.getMember().get();

		return this.cancelOrDo(event.getMessage(),
				Flux.fromIterable(players)
						.map(BlackjackPlayer::getUserId)
						.collectList()
						.filter(playerIds -> playerIds.contains(member.getId()))
						.filter(playerIds -> !rateLimiter.isLimitedAndWarn(event.getClient(), member.getGuildId(), event.getMessage().getChannelId(), member.getId()))
						.map(playerIds -> players.stream().filter(player -> player.getUserId().equals(member.getId())).findAny().get())
						.flatMap(player -> {
							if(player.isStanding()) {
								return BotUtils.sendMessage(
										String.format(Emoji.GREY_EXCLAMATION + " (**%s**) You're standing, you can't play anymore.",
												member.getUsername()), this.getContext().getChannel())
										.thenReturn(false);
							}

							final String content = event.getMessage().getContent().get().toLowerCase().trim();
							if("double down".equals(content) && player.getCards().size() != 2) {
								return BotUtils.sendMessage(
										String.format(Emoji.GREY_EXCLAMATION + " (**%s**) You must have a maximum of 2 cards to use `double down`.",
												member.getUsername()), this.getContext().getChannel())
										.thenReturn(true);
							}

							Map<String, Runnable> actionsMap = Map.of("hit", player::hit, "stand", player::stand, "double down", player::doubleDown);

							Runnable action = actionsMap.get(content);
							if(action == null) {
								return Mono.just(false);
							}

							action.run();
							return this.stopOrShow()
									.thenReturn(true);
						}));
	}

}