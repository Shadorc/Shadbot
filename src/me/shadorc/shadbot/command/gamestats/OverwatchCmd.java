package me.shadorc.shadbot.command.gamestats;

import java.io.IOException;
import java.util.List;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.MathUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import net.shadorc.overwatch4j.HeroDesc;
import net.shadorc.overwatch4j.Overwatch4J;
import net.shadorc.overwatch4j.OverwatchException;
import net.shadorc.overwatch4j.OverwatchPlayer;
import net.shadorc.overwatch4j.enums.Platform;
import net.shadorc.overwatch4j.enums.Region;
import net.shadorc.overwatch4j.enums.TopHeroesStats;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

@RateLimited
@Command(category = CommandCategory.GAMESTATS, names = { "overwatch" }, alias = "ow")
public class OverwatchCmd extends AbstractCommand {

	static {
		Overwatch4J.setTimeout(Config.DEFAULT_TIMEOUT);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalArgumentException {
		List<String> splitArgs = StringUtils.split(context.getArg());
		if(!MathUtils.inRange(splitArgs.size(), 1, 3)) {
			throw new MissingArgumentException();
		}

		try {
			String username = null;
			Platform platform = null;
			Region region = null;
			switch (splitArgs.size()) {
				case 1:
					username = splitArgs.get(0);
					break;

				case 2:
					platform = this.getPlatform(splitArgs.get(0));
					username = splitArgs.get(1);
					break;

				default:
					platform = this.getPlatform(splitArgs.get(0));
					region = this.getRegion(splitArgs.get(1));
					username = splitArgs.get(2);
					break;
			}

			OverwatchPlayer player = new OverwatchPlayer(username, platform, region);

			EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
					.setLenient(true)
					.withAuthorName("Overwatch Stats")
					.withAuthorIcon("http://vignette4.wikia.nocookie.net/overwatch/images/b/bd/Overwatch_line_art_logo_symbol-only.png")
					.withUrl(player.getProfileURL())
					.withThumbnail(player.getIconUrl())
					.appendDescription(String.format("Stats for user **%s**%s",
							player.getName(), player.getRegion() == Region.NONE ? "" : " (Region: " + player.getRegion() + ")"))
					.appendField("Level", Integer.toString(player.getLevel()), true)
					.appendField("Competitive rank", Integer.toString(player.getRank()), true)
					.appendField("Wins", Integer.toString(player.getWins()), true)
					.appendField("Game time", player.getTimePlayed(), true)
					.appendField("Top hero (Time played)", this.getTopThreeHeroes(player.getList(TopHeroesStats.TIME_PLAYED)), true)
					.appendField("Top hero (Eliminations per life)", this.getTopThreeHeroes(player.getList(TopHeroesStats.ELIMINATIONS_PER_LIFE)), true);
			BotUtils.sendMessage(builder.build(), context.getChannel());

		} catch (OverwatchException err) {
			String msg;
			switch (err.getType()) {
				case BLIZZARD_INTERNAL_ERROR:
					msg = "There's an internal error on the Blizzard side, please try again later.";
					break;
				case NO_DATA:
					msg = "There is no data for this account yet.";
					break;
				case USER_NOT_FOUND:
					msg = "This user doesn't play Overwatch or doesn't exist.";
					break;
				default:
					msg = "An unknown error occurred while getting information from Overwatch profile.";
					break;
			}
			BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " " + msg, context.getChannel());
		} catch (IOException err) {
			ExceptionUtils.handle("getting information from Overwatch profile", context, err);
		}
	}

	private String getTopThreeHeroes(List<HeroDesc> heroesList) {
		return FormatUtils.numberedList(3, heroesList.size(), count -> String.format("**%s**. %s (%s)",
				count, heroesList.get(count - 1).getName(), heroesList.get(count - 1).getDesc()));
	}

	private Platform getPlatform(String str) {
		Platform platform = Utils.getValueOrNull(Platform.class, str.toUpperCase());
		if(platform == null) {
			throw new IllegalArgumentException("Invalid platform. Options: "
					+ FormatUtils.formatArray(Platform.values(), plat -> plat.toString(), ", "));
		}
		return platform;
	}

	private Region getRegion(String str) {
		Region region = Utils.getValueOrNull(Region.class, str.toUpperCase());
		if(region == null) {
			throw new IllegalArgumentException("Invalid region. Options: "
					+ FormatUtils.formatArray(Region.values(), reg -> reg.toString(), ", "));
		}
		return region;
	}

	@Override
	public EmbedObject getHelp(Context context) {
		return new HelpBuilder(this, context.getPrefix())
				.setDescription("Show player's stats for Overwatch.")
				.addArg(Utils.removeAndGet(Platform.values(), Platform.NONE), true)
				.addArg(Utils.removeAndGet(Region.values(), Region.NONE), true)
				.addArg("battletag#0000", false)
				.appendField("Info", "**platform** and **region** are automatically detected if nothing is specified.", false)
				.build();
	}

}
