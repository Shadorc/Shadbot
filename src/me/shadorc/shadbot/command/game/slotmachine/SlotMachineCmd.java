package me.shadorc.shadbot.command.game.slotmachine;

import java.util.List;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.core.ratelimiter.RateLimiter;
import me.shadorc.shadbot.data.db.DBMember;
import me.shadorc.shadbot.data.db.DatabaseManager;
import me.shadorc.shadbot.data.stats.MoneyStatsManager;
import me.shadorc.shadbot.data.stats.MoneyStatsManager.MoneyEnum;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.Utils;
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
	public Mono<Void> execute(Context context) {
		DBMember dbMember = DatabaseManager.getDBMember(context.getGuildId().get(), context.getAuthorId());

		if(dbMember.getCoins() < PAID_COST) {
			return context.getAuthor()
					.flatMap(author -> BotUtils.sendMessage(TextUtils.notEnoughCoins(author), context.getChannel()))
					.then();
		}

		final List<SlotOptions> slots = List.of(Utils.randValue(SLOTS_ARRAY), Utils.randValue(SLOTS_ARRAY), Utils.randValue(SLOTS_ARRAY));
		final int gains = slots.stream().distinct().count() == 1 ? slots.get(0).getGain() : -PAID_COST;

		dbMember.addCoins(gains);
		if(gains > 0) {
			MoneyStatsManager.log(MoneyEnum.MONEY_GAINED, this.getName(), gains);
		} else {
			MoneyStatsManager.log(MoneyEnum.MONEY_LOST, this.getName(), Math.abs(gains));
		}

		return context.getAuthorName()
				.flatMap(username -> BotUtils.sendMessage(String.format("%s%n(**%s**) You %s **%s** !",
						FormatUtils.format(slots, SlotOptions::getEmoji, " "), username,
						gains > 0 ? "win" : "lose", FormatUtils.formatCoins(Math.abs(gains))), context.getChannel()))
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Play slot machine.")
				.addField("Cost", String.format("A game costs **%d coins**.", PAID_COST), false)
				.build();
	}

}