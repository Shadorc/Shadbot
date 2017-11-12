package me.shadorc.discordbot.command.game;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.Timer;

import org.joda.time.Instant;
import org.json.JSONArray;
import org.json.JSONObject;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.data.LottoDataManager;
import me.shadorc.discordbot.data.StatCategory;
import me.shadorc.discordbot.data.StatsManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.TextUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class LottoCmd extends AbstractCommand implements ActionListener {

	private static final int PAID_COST = 100;

	private Timer timer;

	public LottoCmd() {
		super(CommandCategory.GAME, Role.USER, RateLimiter.DEFAULT_COOLDOWN, "lotto");
		this.timer = new Timer(this.getDelayBeforeNextDraw(), this);
		this.timer.start();
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			EmbedBuilder builder = Utils.getDefaultEmbed()
					.withAuthorName("Lotto")
					.withThumbnail("https://cdn.onlineunitedstatescasinos.com/wp-content/uploads/2016/04/Lottery-icon.png")
					.withDescription(this.getDelaySentance()
							+ "\nTo participate, type: `" + context.getPrefix() + this.getFirstName() + " 1-100`")
					.appendField("Number of participants", Integer.toString(LottoDataManager.getPlayers().length()), false)
					.appendField("Prize pool", StringUtils.formatNum(LottoDataManager.getPool()) + " coins", false);

			if(this.getNum(context.getAuthor()) != -1) {
				builder.withFooterIcon("https://images.emojiterra.com/twitter/512px/1f39f.png");
				builder.withFooterText(context.getAuthorName() + ", you bet on number " + this.getNum(context.getAuthor()) + ".");
			}

			JSONObject historicObj = LottoDataManager.getHistoric();
			if(historicObj != null) {
				StringBuilder strBuilder = new StringBuilder("Last week, the prize pool contained **"
						+ StringUtils.formatCoins(LottoDataManager.getHistoric().getInt(LottoDataManager.HISTORIC_POOL))
						+ "**, the winning number was **"
						+ LottoDataManager.getHistoric().getInt(LottoDataManager.HISTORIC_NUM)
						+ "** and **");

				int winnerCount = LottoDataManager.getHistoric().getInt(LottoDataManager.HISTORIC_WINNERS_COUNT);
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

		if(!StringUtils.isIntBetween(context.getArg(), 1, 100)) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid number, must be between 1 and 100.", context.getChannel());
			return;
		}

		int num = Integer.parseInt(context.getArg());

		DatabaseManager.addCoins(context.getGuild(), context.getAuthor(), -PAID_COST);
		StatsManager.increment(StatCategory.MONEY_LOSSES_COMMAND, this.getFirstName(), PAID_COST);
		LottoDataManager.addToPool(PAID_COST);

		LottoDataManager.addPlayer(context.getGuild(), context.getAuthor(), num);

		BotUtils.sendMessage(Emoji.TICKET + " You bought a lottery ticket and bet on number **" + num + "**. Good luck !", context.getChannel());
	}

	private int getNum(IUser user) {
		JSONArray players = LottoDataManager.getPlayers();
		for(int i = 0; i < players.length(); i++) {
			if(players.getJSONObject(i).getLong(LottoDataManager.USER_ID) == user.getLongID()) {
				return players.getJSONObject(i).getInt(LottoDataManager.NUM);
			}
		}
		return -1;
	}

	private String getDelaySentance() {
		int minutes = this.getDelayBeforeNextDraw() / 1000 / 60;
		int hours = minutes / 60;
		int days = hours / 24;
		return "The next draw will take place in "
				+ (days > 0 ? StringUtils.pluralOf(days, "day") + " " : "")
				+ (hours > 0 ? StringUtils.pluralOf(hours % 24, "hour") + " and " : "")
				+ StringUtils.pluralOf(minutes % 60, "minute") + ". ";
	}

	private int getDelayBeforeNextDraw() {
		ZonedDateTime nextDate = ZonedDateTime.now()
				.with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
				.withHour(12)
				.withMinute(0)
				.withSecond(0);
		return (int) (nextDate.toInstant().toEpochMilli() - Instant.now().getMillis());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Buy a ticket for the lottery or display the current lottery status.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " [<num>]`", false)
				.appendField("Restrictions", "**num** - must be between 1 and 100", false)
				.appendField("Info", "One winner is randomly drawn every Sunday at noon (English time)."
						+ "\nIf no one wins, the prize pool is put back into play, "
						+ "if there are multiple winners, the prize pool is splitted between them.", false)
				.appendField("Gains", "The prize pool contains all coins lost at games during the week plus "
						+ "the purchase price of the lottery tickets.", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		int winningNum = MathUtils.rand(1, 100);

		List<JSONObject> winnersList = Utils.convertToList(LottoDataManager.getPlayers(), JSONObject.class);
		winnersList = winnersList.stream().filter(
				playerObj -> playerObj.getInt(LottoDataManager.NUM) == winningNum
						&& Shadbot.getClient().getGuildByID(playerObj.getLong(LottoDataManager.GUILD_ID)) != null
						&& Shadbot.getClient().getUserByID(playerObj.getLong(LottoDataManager.USER_ID)) != null)
				.collect(Collectors.toList());

		for(JSONObject winnerObj : winnersList) {
			IGuild guild = Shadbot.getClient().getGuildByID(winnerObj.getLong(LottoDataManager.GUILD_ID));
			IUser user = Shadbot.getClient().getUserByID(winnerObj.getLong(LottoDataManager.USER_ID));
			int coins = (int) Math.ceil((double) LottoDataManager.getPool() / winnersList.size());
			DatabaseManager.addCoins(guild, user, coins);
			StatsManager.increment(StatCategory.MONEY_GAINS_COMMAND, this.getFirstName(), coins);
		}

		LottoDataManager.setHistoric(winnersList.size(), LottoDataManager.getPool(), winningNum);

		if(!winnersList.isEmpty()) {
			LottoDataManager.resetPool();
		}
		LottoDataManager.resetUsers();

		this.timer = new Timer(this.getDelayBeforeNextDraw(), this);
	}
}
