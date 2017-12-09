package me.shadorc.discordbot.command.hidden;

import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.data.JSONKey;
import me.shadorc.discordbot.data.PremiumManager;
import me.shadorc.discordbot.data.PremiumManager.RelicType;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import sx.blah.discord.util.EmbedBuilder;

public class ContributorStatusCmd extends AbstractCommand {

	public ContributorStatusCmd() {
		super(CommandCategory.HIDDEN, Role.USER, "contributor_status", "donator_status", "relic_status");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		JSONArray keysArray = PremiumManager.getKeysForUser(context.getAuthor().getLongID());
		if(keysArray == null) {
			BotUtils.sendMessage(Emoji.INFO + " You are not a contributor. If you like Shadbot, please consider donating on " + Config.PATREON_URL + "."
					+ "\nAll donations are important and help me keeping Shadbot alive. Thanks !", context.getChannel());
			return;
		}

		EmbedBuilder builder = Utils.getDefaultEmbed()
				.withAuthorName("Contributor status");

		for(int i = 0; i < keysArray.length(); i++) {
			JSONObject keyObj = keysArray.getJSONObject(i);

			StringBuilder contentBld = new StringBuilder("**ID:** " + keyObj.getString(JSONKey.RELIC_ID.toString())
					+ "\n**Duration:** " + keyObj.getInt(JSONKey.RELIC_DURATION.toString()) + " days");

			StringBuilder titleBld = new StringBuilder();
			if(keyObj.getString(JSONKey.RELIC_TYPE.toString()).equals(RelicType.GUILD.toString())) {
				titleBld.append("Legendary ");
			}
			titleBld.append("Relic");

			if(keyObj.getBoolean(JSONKey.RELIC_EXPIRED.toString())) {
				titleBld.append(" (Expired)");
			} else if(keyObj.optLong(JSONKey.RELIC_ACTIVATION_MILLIS.toString()) == 0) {
				titleBld.append(" (Not activated)");
				builder.withFooterText("To activate a relic, use " + context.getPrefix() + "activate_relic <relicID>");
			} else {
				titleBld.append(" (Activated)");

				long expriationMs = TimeUnit.DAYS.toMillis(keyObj.getInt(JSONKey.RELIC_DURATION.toString()));
				long activationMs = keyObj.getLong(JSONKey.RELIC_ACTIVATION_MILLIS.toString());
				long expireMs = expriationMs - (System.currentTimeMillis() - activationMs);
				contentBld.append("\n**Expires in:** " + TimeUnit.MILLISECONDS.toDays(expireMs) + " days");
			}

			builder.appendField(titleBld.toString(), contentBld.toString(), false);
		}

		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show your contributor status.**");
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
