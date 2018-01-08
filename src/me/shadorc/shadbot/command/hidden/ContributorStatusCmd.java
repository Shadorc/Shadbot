package me.shadorc.shadbot.command.hidden;

import java.util.List;
import java.util.concurrent.TimeUnit;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.data.premium.PremiumManager;
import me.shadorc.shadbot.data.premium.Relic;
import me.shadorc.shadbot.data.premium.RelicType;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DateUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

@Command(category = CommandCategory.HIDDEN, names = { "contributor_status", "donator_status", "relic_status" })
public class ContributorStatusCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		List<Relic> relics = PremiumManager.getRelicsForUser(context.getAuthor().getLongID());
		if(relics.isEmpty()) {
			BotUtils.sendMessage(String.format(Emoji.INFO + " You are not a contributor. If you like Shadbot, please consider donating on %s."
					+ "\nAll donations are important and help me keeping Shadbot alive. Thanks !", Config.PATREON_URL), context.getChannel());
			return;
		}

		EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
				.withAuthorName("Contributor status")
				.withThumbnail("https://orig00.deviantart.net/24e1/f/2015/241/8/7/relic_fragment_by_yukimemories-d97l8c8.png");

		for(Relic relic : relics) {
			StringBuilder contentBld = new StringBuilder();
			contentBld.append(String.format("**ID:** %s", relic.getID()));
			if(relic.getType().equals(RelicType.GUILD)) {
				contentBld.append(String.format("%n**Guild ID:** %d", relic.getGuildID()));
			}
			contentBld.append(String.format("%n**Duration:** %d days", relic.getDuration()));
			if(!relic.isExpired()) {
				long daysLeft = relic.getDuration() - TimeUnit.MILLISECONDS.toDays(DateUtils.getMillisUntil(relic.getActivationTime()));
				contentBld.append(String.format("%n**Expires in:** %d days", daysLeft));
			}

			StringBuilder titleBld = new StringBuilder();
			if(relic.getType().equals(RelicType.GUILD)) {
				titleBld.append("Legendary ");
			}
			titleBld.append(String.format("Relic (%s)", relic.isExpired() ? "Expired" : "Activated"));

			embed.appendField(titleBld.toString(), contentBld.toString(), false);
		}

		BotUtils.sendMessage(embed.build(), context.getChannel());
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show your contributor status.")
				.build();
	}
}
