package me.shadorc.discordbot.command.french;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Log;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.TwitterUtils;
import sx.blah.discord.util.EmbedBuilder;
import twitter4j.TwitterException;

public class HolidaysCmd extends Command {

	public HolidaysCmd() {
		super(false, "vacs", "vacances");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(context.getArg() == null) {
			throw new MissingArgumentException();
		}

		try {
			TwitterUtils.connection();
			String holidays = TwitterUtils.getInstance().getUserTimeline("Vacances_Zone" + context.getArg().toUpperCase()).get(0).getText().replaceAll("#", "");
			BotUtils.sendMessage(Emoji.BEACH + " " + holidays, context.getChannel());
		} catch (TwitterException e) {
			if(e.getErrorCode() == 34) {
				throw new MissingArgumentException();
			} else {
				Log.error("An error occured while getting holidays information.", e, context.getChannel());
			}
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for /" + this.getNames()[0])
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show the number of remaining days before the next school holidays for the indicated zone.**")
				.appendField("Usage", "/vacs <A|B|C>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
