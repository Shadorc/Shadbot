package me.shadorc.shadbot.command.game.roulette;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.game.AbstractGameManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.MathUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.Pair;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class RouletteManager extends AbstractGameManager {

	protected static final List<Integer> RED_NUMS = Arrays.asList(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36);

	private static final int GAME_DURATION = 30;

	private final IChannel channel;
	private final IUser user;
	private final ConcurrentHashMap<IUser, Pair<Integer, String>> playersPlace;

	public RouletteManager(AbstractCommand cmd, IChannel channel, IUser user) {
		super(cmd);
		this.channel = channel;
		this.user = user;
		this.playersPlace = new ConcurrentHashMap<>();
	}

	@Override
	public void start() {
		EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
				.withAuthorName("Roulette Game")
				.withThumbnail("http://icongal.com/gallery/image/278586/roulette_baccarat_casino.png")
				.appendField(String.format("%s started a Roulette game.", user.getName()),
						String.format("Use `%s%s <bet> <place>` to join the game."
								+ "%n%n**Place** must be a number between `1 and 36`, `red`, `black`, `even`, `odd`, `low` or `high`",
								Database.getDBGuild(channel.getGuild()).getPrefix(), this.getCmdName()), false)
				.withFooterText(String.format("You have %d seconds to make your bets.", GAME_DURATION));
		BotUtils.sendMessage(embed.build(), channel).get();
		this.schedule(() -> this.stop(), GAME_DURATION, TimeUnit.SECONDS);
	}

	@Override
	public void stop() {
		this.cancelScheduledTask();

		int winningPlace = MathUtils.rand(1, 36);

		List<String> list = new ArrayList<>();
		for(IUser user : playersPlace.keySet()) {
			int gains = playersPlace.get(user).getFirst();
			String place = playersPlace.get(user).getSecond();

			Map<String, Boolean> testsMap = new HashMap<>();
			testsMap.put("red", RED_NUMS.contains(winningPlace));
			testsMap.put("black", !RED_NUMS.contains(winningPlace));
			testsMap.put("low", MathUtils.inRange(winningPlace, 1, 19));
			testsMap.put("high", MathUtils.inRange(winningPlace, 19, 37));
			testsMap.put("even", winningPlace % 2 == 0);
			testsMap.put("odd", winningPlace % 2 != 0);

			int multiplier = 0;
			if(place.equals(Integer.toString(winningPlace))) {
				multiplier = 36;
			} else if(testsMap.get(place)) {
				multiplier = 2;
			} else {
				multiplier = -1;
			}

			gains *= multiplier;
			if(gains > 0) {
				list.add(0, String.format("**%s** (Gains: **%s)**", user.getName(), FormatUtils.formatCoins(gains)));
			} else {
				list.add(String.format("**%s** (Losses: **%s)**", user.getName(), FormatUtils.formatCoins(Math.abs(gains))));
			}
			Database.getDBUser(channel.getGuild(), user).addCoins(gains);
			// StatsManager.increment(CommandManager.getFirstName(context.getCommand()), gains);
		}

		BotUtils.sendMessage(String.format(Emoji.DICE + " No more bets. *The wheel is spinning...* **%d (%d)** !"
				+ "\n" + Emoji.BANK + " __Results:__ %s.",
				winningPlace, RED_NUMS.contains(winningPlace) ? "Red" : "Black", FormatUtils.formatList(list, Object::toString, ", ")),
				channel);

		playersPlace.clear();
		RouletteCmd.MANAGERS.remove(channel.getLongID());
	}

	protected boolean addPlayer(IUser user, Integer bet, String place) {
		return playersPlace.putIfAbsent(user, new Pair<Integer, String>(bet, place)) == null;
	}

}