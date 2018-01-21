package me.shadorc.shadbot.command.game.dice;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.game.AbstractGameManager;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.data.stats.Stats.MoneyEnum;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.UpdateableMessage;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class DiceManager extends AbstractGameManager {

	private static final int GAME_DURATION = 30;

	private final ConcurrentHashMap<Integer, IUser> numsPlayers;
	private final int bet;
	private final UpdateableMessage message;

	private String results;

	public DiceManager(AbstractCommand cmd, IChannel channel, IUser author, int bet) {
		super(cmd, channel, author);
		this.bet = bet;
		this.numsPlayers = new ConcurrentHashMap<>();
		this.message = new UpdateableMessage(channel);
	}

	@Override
	public void start() {
		this.schedule(() -> this.stop(), GAME_DURATION, TimeUnit.SECONDS);
	}

	private void show() {
		EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
				.setLenient(true)
				.withAuthorName("Dice Game")
				.withThumbnail("http://findicons.com/files/icons/2118/nuvola/128/package_games_board.png")
				.withDescription(String.format("**Use `%s%s <num>` to join the game.**%n**Bet:** %s",
						this.getPrefix(), this.getCmdName(), FormatUtils.formatCoins(bet)))
				.appendField("Player", numsPlayers.values().stream().map(IUser::getName).collect(Collectors.joining("\n")), true)
				.appendField("Number", numsPlayers.keySet().stream().map(Object::toString).collect(Collectors.joining("\n")), true)
				.appendField("Results", results, false)
				.withFooterText(String.format("You have %d seconds to make your bets.", GAME_DURATION));
		message.send(embed.build());
	}

	@Override
	public void stop() {
		this.cancelScheduledTask();

		int winningNum = ThreadLocalRandom.current().nextInt(1, 7);

		List<String> list = new ArrayList<>();
		for(int num : numsPlayers.keySet()) {
			IUser user = numsPlayers.get(num);
			int gains = bet;
			if(num == winningNum) {
				gains *= numsPlayers.size() + DiceCmd.MULTIPLIER;
				StatsManager.increment(MoneyEnum.MONEY_GAINED, this.getCmdName(), gains);
			} else {
				gains *= -1;
				StatsManager.increment(MoneyEnum.MONEY_LOST, this.getCmdName(), Math.abs(gains));
			}
			list.add(gains > 0 ? 0 : list.size(), String.format("%s (**%s**)", user.getName(), FormatUtils.formatCoins(gains)));

			Database.getDBUser(this.getGuild(), user).addCoins(gains);
		}

		BotUtils.sendMessage(String.format(Emoji.DICE + " The dice is rolling... **%s** !", winningNum), this.getChannel()).get();

		this.results = FormatUtils.format(list, Object::toString, "\n");
		this.show();

		numsPlayers.clear();
		DiceCmd.MANAGERS.remove(this.getChannel().getLongID());
	}

	public int getBet() {
		return bet;
	}

	public int getPlayersCount() {
		return numsPlayers.size();
	}

	public boolean addPlayer(IUser user, int num) {
		if(numsPlayers.putIfAbsent(num, user) == null) {
			this.show();
			return true;
		}
		return false;
	}

	protected boolean isNumBet(int num) {
		return numsPlayers.containsKey(num);
	}
}
