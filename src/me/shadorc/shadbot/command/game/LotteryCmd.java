package me.shadorc.shadbot.command.game;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.database.DBMember;
import me.shadorc.shadbot.data.lottery.LotteryGambler;
import me.shadorc.shadbot.data.lottery.LotteryHistoric;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.exception.ExceptionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class LotteryCmd extends BaseCmd {

	private static final int PAID_COST = 100;
	private static final int MIN_NUM = 1;
	private static final int MAX_NUM = 100;

	public LotteryCmd() {
		super(CommandCategory.GAME, List.of("lottery", "lotto"));
		this.setDefaultRateLimiter();
	}

	@Override
	public Mono<Void> execute(Context context) {
		if(!context.getArg().isPresent()) {
			return this.show(context).then();
		}

		final String arg = context.requireArg();

		final DBMember dbMember = Shadbot.getDatabase().getDBMember(context.getGuildId(), context.getAuthorId());
		if(dbMember.getCoins() < PAID_COST) {
			return Mono.error(new CommandException(TextUtils.NOT_ENOUGH_COINS));
		}

		final LotteryGambler gambler = Shadbot.getLottery().getGamblers().stream()
				.filter(lotteryGambler -> lotteryGambler.getUserId().equals(context.getAuthorId()))
				.findAny()
				.orElse(null);

		if(gambler != null) {
			return Mono.error(new CommandException("You're already participating."));
		}

		final Integer num = NumberUtils.asIntBetween(arg, MIN_NUM, MAX_NUM);
		if(num == null) {
			return Mono.error(new CommandException(String.format("`%s` is not a valid number, it must be between **%d** and **%d**.",
					arg, MIN_NUM, MAX_NUM)));
		}

		dbMember.addCoins(-PAID_COST);

		Shadbot.getLottery().addGambler(context.getGuildId(), context.getAuthorId(), num);

		return context.getChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.TICKET + " (**%s**) You bought a lottery ticket and bet on number **%d**. Good luck ! "
						+ "The next draw will take place in **%s**.",
						context.getUsername(), num, FormatUtils.customDate(LotteryCmd.getDelay().toMillis())), channel))
				.then();
	}

	private Mono<Message> show(Context context) {
		final List<LotteryGambler> gamblers = Shadbot.getLottery().getGamblers();

		final Consumer<EmbedCreateSpec> embedConsumer = EmbedUtils.getDefaultEmbed()
				.andThen(embed -> {
					embed.setAuthor("Lottery", null, context.getAvatarUrl())
							.setThumbnail("https://cdn.onlineunitedstatescasinos.com/wp-content/uploads/2016/04/Lottery-icon.png")
							.setDescription(String.format("The next draw will take place in **%s**%nTo participate, type: `%s%s %d-%d`",
									FormatUtils.customDate(LotteryCmd.getDelay().toMillis()),
									context.getPrefix(), this.getName(), MIN_NUM, MAX_NUM))
							.addField("Number of participants", Integer.toString(gamblers.size()), false)
							.addField("Prize pool", FormatUtils.coins(Shadbot.getLottery().getJackpot()), false);

					final LotteryGambler gambler = gamblers.stream()
							.filter(lotteryGambler -> lotteryGambler.getUserId().equals(context.getAuthorId()))
							.findAny()
							.orElse(null);

					if(gambler != null) {
						embed.setFooter(String.format("You bet on number %d.", gambler.getNumber()),
								"https://images.emojiterra.com/twitter/512px/1f39f.png");
					}

					final LotteryHistoric historic = Shadbot.getLottery().getHistoric();
					if(historic != null) {
						String people;
						switch (historic.getWinnerCount()) {
							case 0:
								people = "nobody";
								break;
							case 1:
								people = "one person";
								break;
							default:
								people = historic.getWinnerCount() + " people";
								break;
						}

						embed.addField("Historic",
								String.format("Last week, the prize pool contained **%s**, the winning number was **%d** and **%s won**.",
										FormatUtils.coins(historic.getJackpot()), historic.getNumber(), people),
								false);
					}
				});

		return context.getChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(embedConsumer, channel));
	}

	public static Duration getDelay() {
		ZonedDateTime nextDate = ZonedDateTime.now()
				.with(DayOfWeek.SUNDAY)
				.withHour(12)
				.withMinute(0)
				.withSecond(0);
		if(nextDate.isBefore(ZonedDateTime.now())) {
			nextDate = nextDate.plusWeeks(1);
		}

		return Duration.ofMillis(TimeUtils.getMillisUntil(nextDate.toInstant()));
	}

	public static Mono<Void> draw(DiscordClient client) {
		LogUtils.info("Lottery draw started...");
		final int winningNum = ThreadLocalRandom.current().nextInt(MIN_NUM, MAX_NUM + 1);

		final List<LotteryGambler> winners = Shadbot.getLottery().getGamblers().stream()
				.filter(gambler -> gambler.getNumber() == winningNum)
				.collect(Collectors.toList());

		LogUtils.info("Lottery draw done (Winning number: %d | %d winner(s) | Prize pool: %d)",
				winningNum, winners.size(), Shadbot.getLottery().getJackpot());

		return Flux.fromIterable(winners)
				.flatMap(winner -> client.getMemberById(winner.getGuildId(), winner.getUserId()))
				.flatMap(member -> {
					final int coins = (int) Math.ceil((double) Shadbot.getLottery().getJackpot() / winners.size());
					Shadbot.getDatabase().getDBMember(member.getGuildId(), member.getId()).addCoins(coins);
					return member.getPrivateChannel()
							.cast(MessageChannel.class)
							.flatMap(privateChannel -> DiscordUtils.sendMessage(String.format("Congratulations, you have the winning lottery number! You earn **%s**.",
									FormatUtils.coins(coins)), privateChannel))
							.onErrorResume(ExceptionUtils::isDiscordForbidden, err -> Mono.empty());
				})
				.then(Mono.fromRunnable(() -> {
					Shadbot.getLottery().setHistoric(new LotteryHistoric(Shadbot.getLottery().getJackpot(), winners.size(), winningNum));
					Shadbot.getLottery().resetGamblers();
					if(!winners.isEmpty()) {
						Shadbot.getLottery().resetJackpot();
					}
				}));
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Buy a ticket for the lottery or display the current lottery status.")
				.addArg("num", String.format("must be between %d and %d", MIN_NUM, MAX_NUM), true)
				.addField("Info", "One winner is randomly drawn every Sunday at noon (English time)."
						+ "\nIf no one wins, the prize pool is put back into play, "
						+ "if there are multiple winners, the prize pool is splitted between them.", false)
				.setGains("The prize pool contains all coins lost at games during the week plus the purchase price of the lottery tickets.")
				.build();
	}
}
