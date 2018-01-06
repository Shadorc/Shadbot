package me.shadorc.shadbot.command.game.dice;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.FormatUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.game.AbstractGameManager;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class DiceManager extends AbstractGameManager {

	private static final int GAME_DURATION = 30;

	private final ConcurrentHashMap<Integer, IUser> numsPlayers;
	private final int bet;

	public DiceManager(AbstractCommand cmd, IChannel channel, IUser author, int bet) {
		super(cmd, channel, author);
		this.bet = bet;
		this.numsPlayers = new ConcurrentHashMap<>();
	}

	@Override
	public void start() {
		EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
				.withAuthorName("Dice Game")
				.withThumbnail("http://findicons.com/files/icons/2118/nuvola/128/package_games_board.png")
				.appendField(String.format("%s started a dice game.", this.getAuthor().getName()),
						String.format("Use `%s%s <num>` to join the game with a **%s** putting.",
								Database.getDBGuild(this.getGuild()).getPrefix(), this.getCmdName(), FormatUtils.formatCoins(bet)),
						false)
				.withFooterText(String.format("You have %d seconds to make your bets.", GAME_DURATION));
		BotUtils.sendMessage(embed.build(), this.getChannel()).get();

		this.schedule(() -> this.stop(), GAME_DURATION, TimeUnit.SECONDS);
	}

	@Override
	public void stop() {
		this.cancelScheduledTask();

		int winningNum = MathUtils.rand(1, 6);

		List<String> list = new ArrayList<>();
		for(int num : numsPlayers.keySet()) {
			IUser user = numsPlayers.get(num);
			int gains = bet;
			if(num == winningNum) {
				gains *= numsPlayers.size() + DiceCmd.MULTIPLIER;
				list.add(0, String.format("**%s** (Gains: **%s)**", user.getName(), FormatUtils.formatCoins(gains)));
			} else {
				gains *= -1;
				list.add(String.format("**%s** (Losses: **%s)**", user.getName(), FormatUtils.formatCoins(Math.abs(gains))));
			}

			Database.getDBUser(this.getGuild(), user).addCoins(gains);
			// StatsManager.increment(CommandManager.getFirstName(context.getCommand()), gains);
		}

		BotUtils.sendMessage(String.format(Emoji.DICE + " The dice is rolling... **%s** !%n" + Emoji.BANK + " __Results:__ %s.",
				winningNum, FormatUtils.formatList(list, Object::toString, ", ")),
				this.getChannel());

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
		return numsPlayers.putIfAbsent(num, user) == null;
	}

	protected boolean isNumBet(int num) {
		return numsPlayers.containsKey(num);
	}
}
