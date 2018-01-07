package me.shadorc.shadbot.command.game;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.db.DBUser;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.data.lotto.LottoHistoric;
import me.shadorc.shadbot.data.lotto.LottoManager;
import me.shadorc.shadbot.data.lotto.LottoPlayer;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.CastUtils;
import me.shadorc.shadbot.utils.DateUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.MathUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.executor.ShadbotScheduledExecutor;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

@RateLimited
@Command(category = CommandCategory.GAME, names = { "lotto" })
public class LottoCmd extends AbstractCommand {

	private static final ScheduledThreadPoolExecutor SCHEDULED_EXECUTOR = new ShadbotScheduledExecutor("LottoCmd-%d");

	private static final int PAID_COST = 100;
	private static final int MIN_NUM = 1;
	private final static int MAX_NUM = 100;

	static {
		SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> LottoCmd.lotteryDraw(), LottoCmd.getDelay(), TimeUnit.DAYS.toMillis(7), TimeUnit.MILLISECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		if(!context.hasArg()) {
			this.show(context);
			return;
		}

		DBUser dbUser = Database.getDBUser(context.getGuild(), context.getAuthor());
		if(dbUser.getCoins() < PAID_COST) {
			BotUtils.sendMessage(TextUtils.notEnoughCoins(context.getAuthor()), context.getChannel());
			return;
		}

		LottoPlayer player = LottoManager.getPlayers().stream()
				.filter(lottoPlayer -> lottoPlayer.getUserID() == context.getAuthor().getLongID())
				.findAny().orElse(null);

		if(player != null) {
			throw new IllegalCmdArgumentException("You're already participating.");
		}

		Integer num = CastUtils.asIntBetween(context.getArg(), MIN_NUM, MAX_NUM);
		if(num == null) {
			throw new IllegalCmdArgumentException(String.format("Invalid number, must be between %d and %d.", MIN_NUM, MAX_NUM));
		}

		dbUser.addCoins(-PAID_COST);
		// StatsManager.increment(this.getFirstName(), -PAID_COST);

		LottoManager.addPlayer(context.getGuild(), context.getAuthor(), num);

		BotUtils.sendMessage(String.format(Emoji.TICKET + " You bought a lottery ticket and bet on number **%d**. Good luck !", num),
				context.getChannel());
	}

	private void show(Context context) {
		List<LottoPlayer> players = LottoManager.getPlayers();

		EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
				.withAuthorName("Lotto")
				.withThumbnail("https://cdn.onlineunitedstatescasinos.com/wp-content/uploads/2016/04/Lottery-icon.png")
				.withDescription(String.format("The next draw will take place in **%s**%nTo participate, type: `%s%s %d-%d`",
						FormatUtils.formatDuration(LottoCmd.getDelay()),
						context.getPrefix(), this.getName(), MIN_NUM, MAX_NUM))
				.appendField("Number of participants", Integer.toString(players.size()), false)
				.appendField("Prize pool", FormatUtils.formatCoins(LottoManager.getPool()), false);

		LottoPlayer player = players.stream()
				.filter(lottoPlayer -> lottoPlayer.getUserID() == context.getAuthor().getLongID())
				.findAny().orElse(null);

		if(player != null) {
			embed.withFooterIcon("https://images.emojiterra.com/twitter/512px/1f39f.png");
			embed.withFooterText(String.format("%s, you bet on number %d.", context.getAuthorName(), player.getNum()));
		}

		LottoHistoric historic = LottoManager.getHistoric();
		if(historic != null) {
			String people;
			switch (historic.getWinnersCount()) {
				case 0:
					people = "nobody";
					break;
				case 1:
					people = "one person";
					break;
				default:
					people = historic.getWinnersCount() + " people";
					break;
			}
			embed.appendField("Historic",
					String.format("Last week, the prize pool contained **%s**, the winning number was **%d** and **%s won**.",
							FormatUtils.formatCoins(historic.getPool()), historic.getNum(), people),
					false);
		}

		BotUtils.sendMessage(embed.build(), context.getChannel());
	}

	public static long getDelay() {
		ZonedDateTime nextDate = ZonedDateTime.now()
				.with(DayOfWeek.SUNDAY)
				.withHour(12)
				.withMinute(0)
				.withSecond(0);
		if(nextDate.isBefore(ZonedDateTime.now())) {
			nextDate = nextDate.plusWeeks(1);
		}

		return DateUtils.getMillisUntil(nextDate.toInstant());
	}

	public static void lotteryDraw() {
		LogUtils.infof("Lottery draw started...");
		int winningNum = MathUtils.rand(MIN_NUM, MAX_NUM);

		List<LottoPlayer> winners = LottoManager.getPlayers().stream()
				.filter(player -> player.getNum() == winningNum
						&& Shadbot.getClient().getGuildByID(player.getGuildID()) != null
						&& Shadbot.getClient().getUserByID(player.getUserID()) != null)
				.collect(Collectors.toList());

		for(LottoPlayer winner : winners) {
			IGuild guild = Shadbot.getClient().getGuildByID(winner.getGuildID());
			IUser user = Shadbot.getClient().getUserByID(winner.getUserID());
			int coins = (int) Math.ceil((double) LottoManager.getPool() / winners.size());
			Database.getDBUser(guild, user).addCoins(coins);
			// StatsManager.increment("lotto", coins);
		}

		LogUtils.infof("Lottery draw done (Winning number: %d | %d winner(s) | Prize pool: %d)",
				winningNum, winners.size(), LottoManager.getPool());

		LottoManager.setHistoric(winners.size(), LottoManager.getPool(), winningNum);
		LottoManager.reset();
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Buy a ticket for the lottery or display the current lottery status.")
				.addArg("num", String.format("must be between %d and %d", MIN_NUM, MAX_NUM), true)
				.appendField("Info", "One winner is randomly drawn every Sunday at noon (English time)."
						+ "\nIf no one wins, the prize pool is put back into play, "
						+ "if there are multiple winners, the prize pool is splitted between them.", false)
				.setGains("The prize pool contains all coins lost at games during the week plus the purchase price of the lottery tickets.")
				.build();
	}
}
