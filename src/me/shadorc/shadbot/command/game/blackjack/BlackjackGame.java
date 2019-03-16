package me.shadorc.shadbot.command.game.blackjack;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import discord4j.common.json.EmbedFieldEntity;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.CommandInitializer;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.game.GameCmd;
import me.shadorc.shadbot.core.game.MultiplayerGame;
import me.shadorc.shadbot.core.ratelimiter.RateLimiter;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.MoneyEnum;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.object.casino.Deck;
import me.shadorc.shadbot.object.casino.Hand;
import me.shadorc.shadbot.object.message.UpdateableMessage;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class BlackjackGame extends MultiplayerGame<BlackjackPlayer> {

	private static final float WIN_MULTIPLIER = 1.15f;

	private final RateLimiter rateLimiter;
	private final UpdateableMessage updateableMessage;

	private final Deck deck;
	private final Hand dealerHand;
	private final Map<String, Consumer<BlackjackPlayer>> actions;

	private long startTime;

	public BlackjackGame(GameCmd<BlackjackGame> gameCmd, Context context) {
		super(gameCmd, context, Duration.ofMinutes(1));

		this.rateLimiter = new RateLimiter(1, Duration.ofSeconds(2));
		this.updateableMessage = new UpdateableMessage(context.getClient(), context.getChannelId());

		this.deck = new Deck();
		this.deck.shuffle();
		this.dealerHand = new Hand();

		this.actions = Map.of("hit", player -> player.hit(this.deck.pick()),
				"double down", player -> player.doubleDown(this.deck.pick()),
				"stand", BlackjackPlayer::stand);
	}

	@Override
	public void start() {
		this.dealerHand.deal(this.deck.pick(2));
		while(this.dealerHand.getValue() < 17) {
			this.dealerHand.deal(this.deck.pick());
		}

		this.schedule(this.end());
		this.startTime = System.currentTimeMillis();
		new BlackjackInputs(this.getContext().getClient(), this).subscribe();
	}

	@Override
	public Mono<Void> end() {
		return Flux.fromIterable(this.getPlayers().values())
				.flatMap(player -> Mono.zip(Mono.just(player), player.getUsername(this.getContext().getClient())))
				.map(tuple -> {
					final BlackjackPlayer player = tuple.getT1();
					final String username = tuple.getT2();

					final int dealerValue = this.dealerHand.getValue();
					final int playerValue = player.getHand().getValue();

					// -1 = Lose | 0 = Draw | 1 = Win
					int result;
					if(playerValue > 21) {
						result = -1;
					} else if(dealerValue <= 21) {
						result = Integer.compare(playerValue, dealerValue);
					} else {
						result = 1;
					}

					int gains = 0;
					final StringBuilder text = new StringBuilder();
					switch (result) {
						case 1:
							gains += (int) Math.ceil(player.getBet() * WIN_MULTIPLIER);
							StatsManager.MONEY_STATS.log(MoneyEnum.MONEY_GAINED, CommandInitializer.getCommand(this.getContext().getCommandName()).getName(), gains);
							text.append(String.format("**%s** (Gains: **%s**)", username, FormatUtils.coins(gains)));
							break;
						case -1:
							gains -= player.getBet();
							StatsManager.MONEY_STATS.log(MoneyEnum.MONEY_LOST, CommandInitializer.getCommand(this.getContext().getCommandName()).getName(), Math.abs(gains));
							Shadbot.getLottery().addToJackpot(Math.abs(gains));
							text.append(String.format("**%s** (Losses: **%s**)", username, FormatUtils.coins(Math.abs(gains))));
							break;
						default:
							text.append(String.format("**%s** (Draw)", username));
							break;
					}

					Shadbot.getDatabase().getDBMember(this.getContext().getGuildId(), player.getUserId()).addCoins(gains);
					return text;
				})
				.collectList()
				.flatMap(results -> this.getContext().getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(
								String.format(Emoji.DICE + " __Results:__ %s", String.join(", ", results)), channel)))
				.then(Mono.fromRunnable(this::stop))
				.then(this.show());
	}

	@Override
	public Mono<Void> show() {
		return Flux.fromIterable(this.getPlayers().values())
				.flatMap(player -> player.format(this.getContext().getClient()))
				.collectList()
				.map(hands -> EmbedUtils.getDefaultEmbed()
						.andThen(embed -> {
							final Hand visibleDealerHand = this.isScheduled() ? new Hand(this.dealerHand.getCards().subList(0, 1)) : this.dealerHand;
							embed.setAuthor("Blackjack Game", null, this.getContext().getAvatarUrl())
									.setThumbnail("https://pbs.twimg.com/profile_images/1874281601/BlackjackIcon_400x400.png")
									.setDescription(String.format("**Use `%s%s <bet>` to join the game.**"
											+ "%n%nType `hit` to take another card, `stand` to pass or `double down` to double down.",
											this.getContext().getPrefix(), this.getContext().getCommandName()))
									.addField("Dealer's hand", visibleDealerHand.format(), true);

							if(this.isScheduled()) {
								final Duration remainingDuration = this.getDuration().minusMillis(TimeUtils.getMillisUntil(this.startTime));
								embed.setFooter(String.format("Will automatically stop in %s seconds. Use %scancel to force the stop.",
										remainingDuration.toSeconds(), this.getContext().getPrefix()), null);
							} else {
								embed.setFooter("Finished", null);
							}

							for(EmbedFieldEntity field : hands) {
								embed.addField(field.getName(), field.getValue(), field.isInline());
							}
						}))
				.flatMap(this.updateableMessage::send)
				.then();
	}

	@Override
	public boolean addPlayerIfAbsent(BlackjackPlayer player) {
		player.hit(this.deck.pick());
		player.hit(this.deck.pick());
		return super.addPlayerIfAbsent(player);
	}

	public boolean allPlayersStanding() {
		return this.getPlayers().values().stream().allMatch(BlackjackPlayer::isStanding);
	}

	public RateLimiter getRateLimiter() {
		return this.rateLimiter;
	}

	public Map<String, Consumer<BlackjackPlayer>> getActions() {
		return Collections.unmodifiableMap(this.actions);
	}

}