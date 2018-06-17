package me.shadorc.shadbot.command.game.slotmachine;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.core.ratelimiter.RateLimiter;
import me.shadorc.shadbot.data.db.DBMember;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.data.stats.MoneyStatsManager;
import me.shadorc.shadbot.data.stats.MoneyStatsManager.MoneyEnum;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.publisher.Mono;

@RateLimited(cooldown = RateLimiter.GAME_COOLDOWN, max = 1)
@Command(category = CommandCategory.GAME, names = { "slot_machine", "slot-machine", "slotmachine" }, alias = "sm")
public class SlotMachineCmd extends AbstractCommand {

	private static final int PAID_COST = 10;

	private static final SlotOptions[] SLOTS_ARRAY = new SlotOptions[] {
			SlotOptions.CHERRIES, SlotOptions.CHERRIES, SlotOptions.CHERRIES, SlotOptions.CHERRIES, // Winning chance : 12.5%
			SlotOptions.BELL, SlotOptions.BELL, SlotOptions.BELL, // Winning chance : 5.3%
			SlotOptions.GIFT }; // Winning chance : 0.2%

	@Override
	public void execute(Context context) {
		DBMember dbMember = Database.getDBMember(context.getGuildId().get(), context.getAuthorId());
		if(dbMember.getCoins() < PAID_COST) {
			context.getAuthor().subscribe(author -> BotUtils.sendMessage(TextUtils.notEnoughCoins(author), context.getChannel()));
			return;
		}

		SlotOptions slot1 = SLOTS_ARRAY[ThreadLocalRandom.current().nextInt(SLOTS_ARRAY.length)];
		SlotOptions slot2 = SLOTS_ARRAY[ThreadLocalRandom.current().nextInt(SLOTS_ARRAY.length)];
		SlotOptions slot3 = SLOTS_ARRAY[ThreadLocalRandom.current().nextInt(SLOTS_ARRAY.length)];

		int gains = -PAID_COST;
		if(List.of(slot2, slot3).stream().allMatch(slot1::equals)) {
			gains = slot1.getGain();
		}

		dbMember.addCoins(gains);
		if(gains > 0) {
			MoneyStatsManager.log(MoneyEnum.MONEY_GAINED, this.getName(), gains);
		} else {
			MoneyStatsManager.log(MoneyEnum.MONEY_LOST, this.getName(), Math.abs(gains));
		}

		BotUtils.sendMessage(String.format("%s%nYou %s **%s** !",
				FormatUtils.format(List.of(slot1, slot2, slot3), opt -> String.format(":%s:", opt.toString().toLowerCase()), " "),
				gains > 0 ? "win" : "have lost", FormatUtils.formatCoins(Math.abs(gains))), context.getChannel());
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Play slot machine.")
				.addField("Cost", String.format("A game costs **%d coins**.", PAID_COST), false)
				.build();
	}

}