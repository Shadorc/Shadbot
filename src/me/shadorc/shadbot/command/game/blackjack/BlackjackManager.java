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
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.game.AbstractGameManager;
import me.shadorc.shadbot.core.ratelimiter.RateLimiter;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.MoneyEnum;
import me.shadorc.shadbot.listener.interceptor.MessageInterceptor;
import me.shadorc.shadbot.listener.interceptor.MessageInterceptorManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.Card;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.UpdateableMessage;
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
	public void start() {
		this.dealerCards.addAll(Card.pick(2));
		while(BlackjackUtils.getValue(this.dealerCards) < 17) {
			this.dealerCards.add(Card.pick());
		}

		this.schedule(this.computeResults(), GAME_DURATION, ChronoUnit.SECONDS);
		MessageInterceptorManager.addInterceptor(this.getContext().getChannelId(), this);
		this.startTime = System.currentTimeMillis();
	}

	@Override
	public void stop() {
		this.cancelScheduledTask();
		MessageInterceptorManager.removeInterceptor(this.getContext().getChannelId(), this);
		BlackjackCmd.MANAGERS.remove(this.getContext().getChannelId());
	}

	@Override
	public Mono<Void> show() {
		return Flux.fromIterable(this.players)
				.flatMap(player -> Mono.zip(Mono.just(player), this.getContext().getClient().getUserById(player.getUserId())))
				.map(playerAndUser -> {
					final BlackjackPlayer player = playerAndUser.getT1();
					final User user = playerAndUser.getT2();
					final String stand = player.isStanding() ? " (Stand)" : "";
					final String doubleDown = player.isDoubleDown() ? " (Double down)" : "";

					return new EmbedFieldEntity(String.format("%s's hand%s%s", user.getUsername(), stand, doubleDown),
							BlackjackUtils.formatCards(player.getCards()), true);
				})
				.collectList()
				.zipWith(this.getContext().getAvatarUrl())
				.map(fieldsAndAvatarUrl -> {
					final List<EmbedFieldEntity> fields = fieldsAndAvatarUrl.getT1();
					final String avatarUrl = fieldsAndAvatarUrl.getT2();

					final EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed()
							.setAuthor("Blackjack Game", null, avatarUrl)
							.setThumbnail("https://pbs.twimg.com/profile_images/1874281601/BlackjackIcon_400x400.png")
							.setDescription(String.format("**Use `%s%s <bet>` to join the game.**"
									+ "%n%nType `hit` to take another card, `stand` to pass or `double down` to double down.",
									this.getContext().getPrefix(), this.getContext().getCommandName()))
							.addField("Dealer's hand", BlackjackUtils.formatCards(this.isTaskDone() ? this.dealerCards : this.dealerCards.subList(0, 1)), true);

					if(this.isTaskDone()) {
						embed.setFooter("Finished", null);
					} else {
						final long remainingTime = GAME_DURATION - TimeUnit.MILLISECONDS.toSeconds(TimeUtils.getMillisUntil(this.startTime));
						embed.setFooter(String.format("This game will end automatically in %d seconds.", remainingTime), null);
					}

					fields.stream().forEach(field -> embed.addField(field.getName(), field.getValue(), field.isInline()));

					return embed;
				})
				.flatMap(this.updateableMessage::send)
				.then();
	}

	public Mono<Void> computeResultsOrShow() {
		if(this.isFinished()) {
			return this.computeResults();
		}
		return this.show().then();
	}

	private Mono<Void> computeResults() {
		final int dealerValue = BlackjackUtils.getValue(this.dealerCards);
		return Flux.fromIterable(this.players)
				.flatMap(player -> Mono.zip(Mono.just(player), this.getContext().getClient().getUserById(player.getUserId())))
				.map(playerAndUser -> {
					final BlackjackPlayer player = playerAndUser.getT1();
					final User user = playerAndUser.getT2();
					final int playerValue = BlackjackUtils.getValue(player.getCards());

					// -1 = Lose | 0 = Draw | 1 = Win
					int result;
					if(playerValue > 21) {
						result = -1;
					} else if(dealerValue <= 21) {
						result = Integer.valueOf(playerValue).compareTo(dealerValue);
					} else {
						result = 1;
					}

					int gains = 0;
					String text = "";
					switch (result) {
						case 1:
							gains += (int) Math.ceil(player.getBet() * WIN_MULTIPLIER);
							StatsManager.MONEY_STATS.log(MoneyEnum.MONEY_GAINED, this.getContext().getCommandName(), gains);
							text = String.format("**%s** (Gains: **%s**)", user.getUsername(), FormatUtils.coins(gains));
							break;
						case 0:
							text = String.format("**%s** (Draw)", user.getUsername());
							break;
						case -1:
							gains -= player.getBet();
							StatsManager.MONEY_STATS.log(MoneyEnum.MONEY_LOST, this.getContext().getCommandName(), Math.abs(gains));
							Shadbot.getLotto().addToJackpot(Math.abs(gains));
							text = String.format("**%s** (Losses: **%s**)", user.getUsername(), FormatUtils.coins(Math.abs(gains)));
							break;
					}

					Shadbot.getDatabase().getDBMember(this.getContext().getGuildId(), user.getId()).addCoins(gains);
					return text;
				})
				.collectList()
				.flatMap(results -> BotUtils.sendMessage(
						String.format(Emoji.DICE + " __Results:__ %s", String.join(", ", results)), this.getContext().getChannel()))
				.then(Mono.fromRunnable(this::stop))
				.then(this.show());
	}

	public boolean addPlayerIfAbsent(Snowflake userId, int bet) {
		if(this.players.stream().map(BlackjackPlayer::getUserId).anyMatch(userId::equals)) {
			return false;
		}
		return this.players.add(new BlackjackPlayer(userId, bet));
	}

	public boolean isFinished() {
		return this.players.stream().allMatch(BlackjackPlayer::isStanding);
	}

	@Override
	public Mono<Boolean> isIntercepted(MessageCreateEvent event) {
		final Member member = event.getMember().get();
		return this.cancelOrDo(event.getMessage(),
				Mono.just(this.players)
						// Check if the member is a current player
						.filter(blackjackPlayers -> blackjackPlayers.stream()
								.map(BlackjackPlayer::getUserId)
								.anyMatch(member.getId()::equals))
						.filter(blackjackPlayers -> !this.rateLimiter.isLimitedAndWarn(
								event.getClient(), member.getGuildId(), event.getMessage().getChannelId(), member.getId()))
						// Find the player associated with the user
						.map(blackjackPlayers -> blackjackPlayers.stream()
								.filter(player -> player.getUserId().equals(member.getId()))
								.findFirst()
								.get())
						.flatMap(player -> {
							if(player.isStanding()) {
								return BotUtils.sendMessage(
										String.format(Emoji.GREY_EXCLAMATION + " (**%s**) You're standing, you can't play anymore.",
												member.getUsername()), this.getContext().getChannel())
										.thenReturn(false);
							}

							final String prefix = Shadbot.getDatabase().getDBGuild(event.getGuildId().get()).getPrefix();
							final String content = event.getMessage().getContent().orElse("").replace(prefix, "").toLowerCase().trim();
							if("double down".equals(content) && player.getCards().size() != 2) {
								return BotUtils.sendMessage(
										String.format(Emoji.GREY_EXCLAMATION + " (**%s**) You must have a maximum of 2 cards to use `double down`.",
												member.getUsername()), this.getContext().getChannel())
										.thenReturn(true);
							}

							final Map<String, Runnable> actionsMap = Map.of("hit", player::hit, "stand", player::stand, "double down", player::doubleDown);

							final Runnable action = actionsMap.get(content);
							if(action == null) {
								return Mono.just(false);
							}

							action.run();
							return this.computeResultsOrShow()
									.thenReturn(true);
						}));
	}

}