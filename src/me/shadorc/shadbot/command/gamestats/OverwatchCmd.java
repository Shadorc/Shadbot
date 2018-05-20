package me.shadorc.shadbot.command.gamestats;

import java.io.IOException;
import java.util.List;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import net.shadorc.overwatch4j.HeroDesc;
import net.shadorc.overwatch4j.Overwatch4J;
import net.shadorc.overwatch4j.OverwatchPlayer;
import net.shadorc.overwatch4j.enums.Platform;
import net.shadorc.overwatch4j.enums.TopHeroesStats;
import net.shadorc.overwatch4j.exception.OverwatchException;

@RateLimited
@Command(category = CommandCategory.GAMESTATS, names = { "overwatch" }, alias = "ow")
public class OverwatchCmd extends AbstractCommand {

	static {
		Overwatch4J.setTimeout(Config.DEFAULT_TIMEOUT);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		List<String> splitArgs = StringUtils.split(context.getArg());
		if(!Utils.isInRange(splitArgs.size(), 1, 3)) {
			throw new MissingArgumentException();
		}

		LoadingMessage loadingMsg = new LoadingMessage("Loading Overwatch profile...", context.getChannel());

		try {
			OverwatchPlayer player;

			String username = null;
			Platform platform = null;
			if(splitArgs.size() == 1) {
				username = splitArgs.get(0);
				loadingMsg.send();
				player = new OverwatchPlayer(username);
			} else {
				platform = this.getPlatform(splitArgs.get(0));
				username = splitArgs.get(1);
				loadingMsg.send();
				player = new OverwatchPlayer(username, platform);
			}

			EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
					.setLenient(true)
					.withAuthorName("Overwatch Stats")
					.withAuthorIcon("http://vignette4.wikia.nocookie.net/overwatch/images/b/bd/Overwatch_line_art_logo_symbol-only.png")
					.withAuthorUrl(player.getProfileURL())
					.withThumbnail(player.getIconUrl())
					.appendDescription(String.format("Stats for user **%s**", player.getName()))
					.addField("Level", Integer.toString(player.getLevel()), true)
					.addField("Competitive rank", Integer.toString(player.getRank()), true)
					.addField("Wins", Integer.toString(player.getWins()), true)
					.addField("Game time", player.getTimePlayed(), true)
					.addField("Top hero (Time played)", this.getTopThreeHeroes(player.getList(TopHeroesStats.TIME_PLAYED)), true)
					.addField("Top hero (Eliminations per life)", this.getTopThreeHeroes(player.getList(TopHeroesStats.ELIMINATIONS_PER_LIFE)), true);
			loadingMsg.edit(embed.build());

		} catch (OverwatchException err) {
			loadingMsg.edit(Emoji.MAGNIFYING_GLASS + " " + err.getMessage());
		} catch (IOException err) {
			loadingMsg.delete();
			Utils.handle("getting information from Overwatch profile", context, err);
		}
	}

	private String getTopThreeHeroes(List<HeroDesc> heroesList) {
		return FormatUtils.numberedList(3, heroesList.size(), count -> String.format("**%s**. %s (%s)",
				count, heroesList.get(count - 1).getName(), heroesList.get(count - 1).getDesc()));
	}

	private Platform getPlatform(String str) throws IllegalCmdArgumentException {
		Platform platform = Utils.getValueOrNull(Platform.class, str.toUpperCase());
		if(platform == null) {
			throw new IllegalCmdArgumentException(String.format("`%s` is not a valid Platform. %s",
					str, FormatUtils.formatOptions(Platform.class)));
		}
		return platform;
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show player's stats for Overwatch.")
				.addArg("platform", String.format("user's platform (%s)",
						FormatUtils.format(Platform.values(), platform -> platform.toString().toLowerCase(), ", ")), true)
				.addArg("battletag#0000", false)
				.addField("Info", "**platform** is automatically detected if nothing is specified.", false)
				.build();
	}

}
