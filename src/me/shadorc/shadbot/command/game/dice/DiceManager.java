package me.shadorc.shadbot.command.game.dice;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.game.AbstractGameManager;
import me.shadorc.shadbot.data.db.DatabaseManager;
import me.shadorc.shadbot.data.stats.MoneyStatsManager;
import me.shadorc.shadbot.data.stats.MoneyStatsManager.MoneyEnum;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.message.UpdateableMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class DiceManager extends AbstractGameManager {

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
		this.schedule(() -> this.stop().subscribe(), GAME_DURATION, TimeUnit.SECONDS);
		return Mono.empty();
	}

	protected Mono<Message> show() {
		return this.getContext().getAvatarUrl()
				.zipWith(Flux.fromIterable(numsPlayers.values())
						.flatMap(userId -> this.getContext().getClient().getUserById(userId))
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
							.addField("Player", FormatUtils.format(usernames, Object::toString, "\n"), true)
							.addField("Number", FormatUtils.format(numsPlayers.keySet(), Object::toString, "\n"), true)
							.setFooter(String.format("You have %d seconds to make your bets.", GAME_DURATION), null);

					if(results != null) {
						embed.addField("Results", results, false);
					}

					return embed;
				})
				.flatMap(embed -> updateableMessage.send(embed));
	}

	@Override
	public Mono<Void> stop() {
		this.cancelScheduledTask();
		DiceCmd.MANAGERS.remove(this.getContext().getChannelId());

		int winningNum = ThreadLocalRandom.current().nextInt(1, 7);

		return Flux.fromIterable(numsPlayers.values())
				.flatMap(userId -> this.getContext().getClient().getUserById(userId))
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
				.map(list -> this.results = FormatUtils.format(list, Object::toString, "\n"))
				.then(BotUtils.sendMessage(String.format(Emoji.DICE + " The dice is rolling... **%s** !", winningNum), this.getContext().getChannel()))
				.then(this.show())
				.then();
	}

	public int getBet() {
		return bet;
	}

	public int getPlayersCount() {
		return numsPlayers.size();
	}

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
}
