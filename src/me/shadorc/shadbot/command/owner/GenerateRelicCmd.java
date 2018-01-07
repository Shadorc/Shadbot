package me.shadorc.shadbot.command.owner;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.data.premium.PremiumManager;
import me.shadorc.shadbot.data.premium.Relic;
import me.shadorc.shadbot.data.premium.RelicType;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.api.internal.json.objects.EmbedObject;

@Command(category = CommandCategory.OWNER, permission = CommandPermission.OWNER, names = { "generate_relic" })
public class GenerateRelicCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		RelicType type = Utils.getValueOrNull(RelicType.class, context.getArg());
		if(type == null) {
			throw new IllegalCmdArgumentException(String.format("Invalid type. Options: %s",
					FormatUtils.format(RelicType.values(), relic -> relic.toString().toLowerCase(), ", ")));
		}

		Relic relic = PremiumManager.generateRelic(type);
		BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " %s relic generated: **%s**",
				StringUtils.capitalize(type.toString()), relic.getID()), context.getChannel());
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Generate a relic.")
				.addArg(RelicType.values(), false)
				.build();
	}
}
