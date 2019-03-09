package me.shadorc.shadbot.command.game.slotmachine;

import java.util.List;
import java.util.function.Consumer;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.database.DBMember;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.data.stats.enums.MoneyEnum;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

public class SlotMachineCmd extends BaseCmd {

	private static final int PAID_COST = 10;

	private static final SlotOptions[] SLOTS_ARRAY = new SlotOptions[] {
			SlotOptions.CHERRIES, SlotOptions.CHERRIES, SlotOptions.CHERRIES, SlotOptions.CHERRIES, // Winning chance : 12.5%
			SlotOptions.BELL, SlotOptions.BELL, SlotOptions.BELL, // Winning chance : 5.3%
			SlotOptions.GIFT }; // Winning chance : 0.2%

	public SlotMachineCmd() {
		super(CommandCategory.GAME, List.of("slot_machine", "slot-machine", "slotmachine"), "sm");
		this.setGameRateLimiter();
	}

	@Override
	public Mono<Void> execute(Context context) {
		final DBMember dbMember = Shadbot.getDatabase().getDBMember(context.getGuildId(), context.getAuthorId());

		if(dbMember.getCoins() < PAID_COST) {
			return Mono.error(new CommandException(TextUtils.NOT_ENOUGH_COINS));
		}

		final List<SlotOptions> slots = List.of(Utils.randValue(SLOTS_ARRAY), Utils.randValue(SLOTS_ARRAY), Utils.randValue(SLOTS_ARRAY));
		final int gains = slots.stream().distinct().count() == 1 ? slots.get(0).getGain() : -PAID_COST;

		dbMember.addCoins(gains);
		if(gains > 0) {
			StatsManager.MONEY_STATS.log(MoneyEnum.MONEY_GAINED, this.getName(), gains);
		} else {
			StatsManager.MONEY_STATS.log(MoneyEnum.MONEY_LOST, this.getName(), Math.abs(gains));
			Shadbot.getLottery().addToJackpot(Math.abs(gains));
		}

		return context.getChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(String.format("%s%n%s (**%s**) You %s **%s** !",
						FormatUtils.format(slots, SlotOptions::getEmoji, " "), Emoji.BANK, context.getUsername(),
						gains > 0 ? "win" : "lose", FormatUtils.coins(Math.abs(gains))), channel))
				.then();
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Play slot machine.")
				.addField("Cost", String.format("A game costs **%d coins**.", PAID_COST), false)
				.build();
	}

}