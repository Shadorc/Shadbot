package me.shadorc.discordbot.command.gamestats;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.Utils;
import net.shadorc.overwatch4j.HeroDesc;
import net.shadorc.overwatch4j.OverwatchPlayer;
import net.shadorc.overwatch4j.enums.Plateform;
import net.shadorc.overwatch4j.enums.Region;
import net.shadorc.overwatch4j.enums.TopHeroesStats;
import net.shadorc.overwatch4j.exceptions.UserNotFoundException;
import sx.blah.discord.util.EmbedBuilder;

public class OverwatchCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public OverwatchCmd() {
		super(Role.USER, "overwatch", "ow");
		this.rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		String[] splitArgs = context.getArg().split(" ");
		if(splitArgs.length < 1 || splitArgs.length > 3) {
			throw new MissingArgumentException();
		}

		try {
			OverwatchPlayer player;

			if(splitArgs.length == 1) {
				String username = splitArgs[0];
				player = new OverwatchPlayer(username);

			} else if(splitArgs.length == 2) {
				String plateform = splitArgs[0].toUpperCase();
				if(!Arrays.stream(Plateform.values()).anyMatch(plateformValue -> plateformValue.toString().equalsIgnoreCase(plateform))) {
					BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid plateform. Options: pc, psn, xbl.", context.getChannel());
					return;
				}

				String username = splitArgs[1];
				player = new OverwatchPlayer(username, Plateform.valueOf(plateform));

			} else {
				String plateform = splitArgs[0].toUpperCase();
				if(!Arrays.stream(Plateform.values()).anyMatch(plateformValue -> plateformValue.toString().equalsIgnoreCase(plateform))) {
					BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid plateform. Options: pc, psn, xbl.", context.getChannel());
					return;
				}

				String region = splitArgs[1].toUpperCase();
				if(!Arrays.stream(Region.values()).anyMatch(regionValue -> regionValue.toString().equalsIgnoreCase(region))) {
					BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid region. Options: eu, us, cn, kr.", context.getChannel());
					return;
				}

				String username = splitArgs[2];
				player = new OverwatchPlayer(username, Plateform.valueOf(plateform), Region.valueOf(region));
			}

			EmbedBuilder builder = new EmbedBuilder()
					.setLenient(true)
					.withAuthorName("Overwatch Stats")
					.withAuthorIcon("http://vignette4.wikia.nocookie.net/overwatch/images/b/bd/Overwatch_line_art_logo_symbol-only.png")
					.withUrl(player.getProfileURL())
					.withThumbnail(player.getIconUrl())
					.withColor(Config.BOT_COLOR)
					.appendDescription("Stats for user **" + player.getName() + "**" + (player.getRegion() == Region.NONE ? "" : " (Region: " + player.getRegion().toString().toUpperCase() + ")"))
					.appendField("Level", Integer.toString(player.getLevel()), true)
					.appendField("Competitive rank", Integer.toString(player.getRank()), true)
					.appendField("Wins", Integer.toString(player.getWins()), true)
					.appendField("Game time", player.getTimePlayed(), true)
					.appendField("Top hero (Time played)", this.getTopThreeHeroes(player.getList(TopHeroesStats.TIME_PLAYED)), true)
					.appendField("Top hero (Eliminations per life)", this.getTopThreeHeroes(player.getList(TopHeroesStats.ELIMINATIONS_PER_LIFE)), true);
			BotUtils.sendEmbed(builder.build(), context.getChannel());

		} catch (UserNotFoundException e) {
			BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " This user doesn't play Overwatch or doesn't exist.", context.getChannel());
		} catch (IOException err) {
			LogUtils.error("Something went wrong while getting information from Overwatch profil.... Please, try again later.", err, context);
		}
	}

	private String getTopThreeHeroes(List<HeroDesc> heroesList) {
		StringBuilder strBuilder = new StringBuilder();
		for(int i = 0; i < Math.min(heroesList.size(), 3); i++) {
			strBuilder.append("**" + (i + 1) + "**. " + heroesList.get(i).getName() + " (" + heroesList.get(i).getDesc() + ")\n");
		}
		return strBuilder.toString();
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show player's stats for Overwatch.**")
				.appendField("Arguments", "**plateform** - optional, automatically detected if nothing is specified."
						+ "\nOptions: pc, xbl, psn"
						+ "\n**region** - optional (only needed if the plateform is PC) automatically detected if nothing is specified."
						+ "\nOptions: us, eu, kr, cn", false)
				.appendField("Usage", "`" + context.getPrefix() + "overwatch [<plateform> <region>] <battletag#0000>`", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
