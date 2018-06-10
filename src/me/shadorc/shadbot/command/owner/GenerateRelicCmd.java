package me.shadorc.shadbot.command.owner;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.data.premium.PremiumManager;
import me.shadorc.shadbot.data.premium.Relic;
import me.shadorc.shadbot.data.premium.RelicType;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;

@Command(category = CommandCategory.OWNER, permission = CommandPermission.OWNER, names = { "generate_relic" })
public class GenerateRelicCmd extends AbstractCommand {

	@Override
	public void execute(Context context) {
		context.requireArg();

		RelicType type = Utils.getValueOrNull(RelicType.class, context.getArg().get());
		if(type == null) {
			throw new IllegalCmdArgumentException(String.format("`%s`in not a valid type. %s",
					context.getArg(), FormatUtils.formatOptions(RelicType.class)));
		}

		Relic relic = PremiumManager.generateRelic(type);
		BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " %s relic generated: **%s**",
				StringUtils.capitalize(type.toString()), relic.getId()), context.getChannel());
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Generate a relic.")
				.addArg(RelicType.values(), false)
				.build();
	}
}
