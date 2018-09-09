package me.shadorc.shadbot.command.owner;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.data.premium.PremiumManager;
import me.shadorc.shadbot.data.premium.Relic;
import me.shadorc.shadbot.data.premium.Relic.RelicType;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import reactor.core.publisher.Mono;

@Command(category = CommandCategory.OWNER, permission = CommandPermission.OWNER, names = { "generate_relic" })
public class GenerateRelicCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		final RelicType type = Utils.getEnum(RelicType.class, context.getArg().get());
		if(type == null) {
			throw new CommandException(String.format("`%s`in not a valid type. %s",
					arg, FormatUtils.options(RelicType.class)));
		}

		final Relic relic = PremiumManager.generateRelic(type);
		return BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " %s relic generated: **%s**",
				StringUtils.capitalize(type.toString()), relic.getId()), context.getChannel())
				.then();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Generate a relic.")
				.addArg(RelicType.values(), false)
				.build();
	}
}
