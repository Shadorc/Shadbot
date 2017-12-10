package me.shadorc.discordbot.command.game;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.joda.time.Instant;
import org.json.JSONArray;
import org.json.JSONObject;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.data.JSONKey;
import me.shadorc.discordbot.data.LottoDataManager;
import me.shadorc.discordbot.exceptions.MissingArgumentException;
import me.shadorc.discordbot.stats.StatsManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.FormatUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.TextUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class LottoCmd extends AbstractCommand {

	private static final int PAID_COST = 100;
	private static final int MIN_NUM = 1;
	private final static int MAX_NUM = 100;

	public LottoCmd() {
		super(CommandCategory.GAME, Role.USER, RateLimiter.DEFAULT_COOLDOWN, "lotto");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			EmbedBuilder builder = Utils.getDefaultEmbed()
					.withAuthorName("Lotto")
					.withThumbnail("https://cdn.onlineunitedstatescasinos.com/wp-content/uploads/2016/04/Lottery-icon.png")
					.withDescription(this.getDelaySentence()
							+ "\nTo participate, type: `" + context.getPrefix() + this.getFirstName() + " " + MIN_NUM + "-" + MAX_NUM + "`")
					.appendField("Number of participants", Integer.toString(LottoDataManager.getPlayers().length()), false)
					.appendField("Prize pool", FormatUtils.formatNum(LottoDataManager.getPool()) + " coins", false);

			if(this.getNum(context.getAuthor()) != -1) {
				builder.withFooterIcon("https://images.emojiterra.com/twitter/512px/1f39f.png");
				builder.withFooterText(context.getAuthorName() + ", you bet on number " + this.getNum(context.getAuthor()) + ".");
			}

			JSONObject historicObj = LottoDataManager.getHistoric();
			if(historicObj != null) {
				StringBuilder strBuilder = new StringBuilder("Last week, the prize pool contained **"
						+ FormatUtils.formatCoins(LottoDataManager.getHistoric().getInt(JSONKey.HISTORIC_POOL.toString()))
						+ "**, the winning number was **"
						+ LottoDataManager.getHistoric().getInt(JSONKey.HISTORIC_NUM.toString())
						+ "** and **");

				int winnerCount = LottoDataManager.getHistoric().getInt(JSONKey.HISTORIC_WINNERS_COUNT.toString());
				if(winnerCount == 0) {
					strBuilder.append("nobody");
				} else if(winnerCount == 1) {
					strBuilder.append("one person");
				} else {
					strBuilder.append(winnerCount + " people");
				}
				strBuilder.append(" won**.");

				builder.appendField("Historic", strBuilder.toString(), false);
			}

			BotUtils.sendMessage(builder.build(), context.getChannel());
			return;
		}

		if(DatabaseManager.getCoins(context.getGuild(), context.getAuthor()) < PAID_COST) {
			BotUtils.sendMessage(TextUtils.notEnoughCoins(context.getAuthor()), context.getChannel());
			return;
		}

		if(this.getNum(context.getAuthor()) != -1) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " You're already participating.", context.getChannel());
			return;
		}

		if(!StringUtils.isIntBetween(context.getArg(), MIN_NUM, MAX_NUM)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid number, must be between " + MIN_NUM + " and " + MAX_NUM + ".", context.getChannel());
			return;
		}

		int num = Integer.parseInt(context.getArg());

		DatabaseManager.addCoins(context.getChannel(), context.getAuthor(), -PAID_COST);
		StatsManager.increment(this.getFirstName(), -PAID_COST);

		LottoDataManager.addPlayer(context.getGuild(), context.getAuthor(), num);

		BotUtils.sendMessage(Emoji.TICKET + " You bought a lottery ticket and bet on number **" + num + "**. Good luck !", context.getChannel());
	}

	private int getNum(IUser user) {
		JSONArray players = LottoDataManager.getPlayers();
		for(int i = 0; i < players.length(); i++) {
			if(players.getJSONObject(i).getLong(JSONKey.USER_ID.toString()) == user.getLongID()) {
				return players.getJSONObject(i).getInt(JSONKey.NUM.toString());
			}
		}
		return -1;
	}

	private String getDelaySentence() {
		int minutes = LottoCmd.getDelayBeforeNextDraw() / 1000 / 60;
		int hours = minutes / 60;
		int days = hours / 24;
		return "The next draw will take place in "
				+ (days > 0 ? StringUtils.pluralOf(days, "day") + " " : "")
				+ (hours > 0 ? StringUtils.pluralOf(hours % 24, "hour") + " and " : "")
				+ StringUtils.pluralOf(minutes % 60, "minute") + ". ";
	}

	public static int getDelayBeforeNextDraw() {
		ZonedDateTime nextDate = ZonedDateTime.now()
				.with(DayOfWeek.SUNDAY)
				.withHour(12)
				.withMinute(0)
				.withSecond(0);
		if(nextDate.isBefore(ZonedDateTime.now())) {
			nextDate = nextDate.plusWeeks(1);
		}
		return (int) (nextDate.toInstant().toEpochMilli() - Instant.now().getMillis());
	}

	public static void lotteryDraw() {
		LogUtils.info("Lottery draw started...");
		int winningNum = MathUtils.rand(MIN_NUM, MAX_NUM);

		List<JSONObject> winnersList = Utils.convertToList(LottoDataManager.getPlayers(), JSONObject.class);
		winnersList = winnersList.stream().filter(
				playerObj -> playerObj.getInt(JSONKey.NUM.toString()) == winningNum
						&& Shadbot.getClient().getGuildByID(playerObj.getLong(JSONKey.GUILD_ID.toString())) != null
						&& Shadbot.getClient().getUserByID(playerObj.getLong(JSONKey.USER_ID.toString())) != null)
				.collect(Collectors.toList());

		for(JSONObject winnerObj : winnersList) {
			IGuild guild = Shadbot.getClient().getGuildByID(winnerObj.getLong(JSONKey.GUILD_ID.toString()));
			IUser user = Shadbot.getClient().getUserByID(winnerObj.getLong(JSONKey.USER_ID.toString()));
			int coins = (int) Math.ceil((double) LottoDataManager.getPool() / winnersList.size());
			DatabaseManager.addCoins(guild, user, coins);
			StatsManager.increment("lotto", coins);
		}

		LottoDataManager.setHistoric(winnersList.size(), LottoDataManager.getPool(), winningNum);
		LottoDataManager.reset();
		LogUtils.info("Lottery draw done.");
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Buy a ticket for the lottery or display the current lottery status.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " [<num>]`", false)
				.appendField("Restrictions", "**num** - must be between " + MIN_NUM + " and " + MAX_NUM, false)
				.appendField("Info", "One winner is randomly drawn every Sunday at noon (English time)."
						+ "\nIf no one wins, the prize pool is put back into play, "
						+ "if there are multiple winners, the prize pool is splitted between them.", false)
				.appendField("Gains", "The prize pool contains all coins lost at games during the week plus "
						+ "the purchase price of the lottery tickets.", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
