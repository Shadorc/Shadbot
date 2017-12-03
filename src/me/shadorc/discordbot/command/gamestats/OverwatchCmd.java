package me.shadorc.discordbot.command.gamestats;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.ExceptionUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import net.shadorc.overwatch4j.HeroDesc;
import net.shadorc.overwatch4j.Overwatch4J;
import net.shadorc.overwatch4j.OverwatchException;
import net.shadorc.overwatch4j.OverwatchPlayer;
import net.shadorc.overwatch4j.enums.Platform;
import net.shadorc.overwatch4j.enums.Region;
import net.shadorc.overwatch4j.enums.TopHeroesStats;
import sx.blah.discord.util.EmbedBuilder;

public class OverwatchCmd extends AbstractCommand {

	public OverwatchCmd() {
		super(CommandCategory.GAMESTATS, Role.USER, RateLimiter.DEFAULT_COOLDOWN, "overwatch");
		this.setAlias("ow");
		Overwatch4J.timeout = Config.DEFAULT_TIMEOUT;
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		String[] splitArgs = StringUtils.getSplittedArg(context.getArg());
		if(splitArgs.length < 1 || splitArgs.length > 3) {
			throw new MissingArgumentException();
		}

		try {
			OverwatchPlayer player;

			if(splitArgs.length == 1) {
				String username = splitArgs[0];
				player = new OverwatchPlayer(username);

			} else if(splitArgs.length == 2) {
				String platform = splitArgs[0].toUpperCase();
				if(!Arrays.stream(Platform.values()).anyMatch(platformValue -> platformValue.toString().equalsIgnoreCase(platform))) {
					BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid platform. Options: pc, psn, xbl.", context.getChannel());
					return;
				}

				String username = splitArgs[1];
				player = new OverwatchPlayer(username, Platform.valueOf(platform));

			} else {
				String platform = splitArgs[0].toUpperCase();
				if(!Arrays.stream(Platform.values()).anyMatch(platformValue -> platformValue.toString().equalsIgnoreCase(platform))) {
					BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid platform. Options: pc, psn, xbl.", context.getChannel());
					return;
				}

				String region = splitArgs[1].toUpperCase();
				if(!Arrays.stream(Region.values()).anyMatch(regionValue -> regionValue.toString().equalsIgnoreCase(region))) {
					BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " Invalid region. Options: eu, us, cn, kr.", context.getChannel());
					return;
				}

				String username = splitArgs[2];
				player = new OverwatchPlayer(username, Platform.valueOf(platform), Region.valueOf(region));
			}

			EmbedBuilder builder = new EmbedBuilder()
					.setLenient(true)
					.withAuthorName("Overwatch Stats")
					.withAuthorIcon("http://vignette4.wikia.nocookie.net/overwatch/images/b/bd/Overwatch_line_art_logo_symbol-only.png")
					.withUrl(player.getProfileURL())
					.withThumbnail(player.getIconUrl())
					.withColor(Config.BOT_COLOR)
					.appendDescription("Stats for user **" + player.getName() + "**"
							+ (player.getRegion() == Region.NONE ? "" : " (Region: " + player.getRegion().toString().toUpperCase() + ")"))
					.appendField("Level", Integer.toString(player.getLevel()), true)
					.appendField("Competitive rank", Integer.toString(player.getRank()), true)
					.appendField("Wins", Integer.toString(player.getWins()), true)
					.appendField("Game time", player.getTimePlayed(), true)
					.appendField("Top hero (Time played)", this.getTopThreeHeroes(player.getList(TopHeroesStats.TIME_PLAYED)), true)
					.appendField("Top hero (Eliminations per life)", this.getTopThreeHeroes(player.getList(TopHeroesStats.ELIMINATIONS_PER_LIFE)), true);
			BotUtils.sendMessage(builder.build(), context.getChannel());

		} catch (OverwatchException err) {
			switch (err.getType()) {
				case BLIZZARD_INTERNAL_ERROR:
					BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " There's an internal error on the Blizzard side, please try again later.", context.getChannel());
					break;
				case NO_DATA:
					BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " There is no data for this account yet.", context.getChannel());
					break;
				case USER_NOT_FOUND:
					BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " This user doesn't play Overwatch or doesn't exist.", context.getChannel());
					break;
			}
		} catch (IOException err) {
			ExceptionUtils.manageException("getting information from Overwatch profile", context, err);
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
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " [<platform> <region>] <battletag#0000>`", false)
				.appendField("Arguments", "**platform** - [OPTIONAL] value: pc, xbl, psn"
						+ "\n**region** - [OPTIONAL] (only needed if the platform is PC) value: us, eu, kr, cn"
						+ "\n**platform** and **region** are automatically detected if nothing is specified.", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

}
