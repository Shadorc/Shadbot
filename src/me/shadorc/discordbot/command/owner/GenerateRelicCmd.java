package me.shadorc.discordbot.command.owner;

import java.util.Arrays;

import org.json.JSONObject;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.JSONKey;
import me.shadorc.discordbot.data.PremiumManager;
import me.shadorc.discordbot.data.PremiumManager.RelicType;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.FormatUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import sx.blah.discord.util.EmbedBuilder;

public class GenerateRelicCmd extends AbstractCommand {

	public GenerateRelicCmd() {
		super(CommandCategory.OWNER, Role.OWNER, "generate_relic");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		String relicTypeStr = context.getArg();
		if(!Arrays.stream(RelicType.values()).anyMatch(type -> type.toString().equalsIgnoreCase(relicTypeStr))) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid type. Options: "
					+ FormatUtils.formatArray(RelicType.values(), relic -> relic.toString().toLowerCase(), ", "), context.getChannel());
			return;
		}

		JSONObject keyObj = PremiumManager.generateRelic(RelicType.valueOf(relicTypeStr.toUpperCase()));
		BotUtils.sendMessage(Emoji.CHECK_MARK + " " + StringUtils.capitalize(relicTypeStr) + " relic generated: **"
				+ keyObj.getString(JSONKey.RELIC_ID.toString()) + "**", context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Generate a relic.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " <type>`", false)
				.appendField("Arguments", "**type** - "
						+ FormatUtils.formatArray(RelicType.values(), relic -> relic.toString().toLowerCase(), ", "), false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
