package me.shadorc.shadbot.command.gamestats;

import java.io.IOException;
import java.util.List;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.message.LoadingMessage;
import net.shadorc.overwatch4j.HeroDesc;
import net.shadorc.overwatch4j.Overwatch4J;
import net.shadorc.overwatch4j.OverwatchPlayer;
import net.shadorc.overwatch4j.enums.Platform;
import net.shadorc.overwatch4j.enums.TopHeroesStats;
import net.shadorc.overwatch4j.exception.OverwatchException;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.GAMESTATS, names = { "overwatch" }, alias = "ow")
public class OverwatchCmd extends AbstractCommand {

	static {
		Overwatch4J.setTimeout(Config.DEFAULT_TIMEOUT);
	}

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(1, 3);

		LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			OverwatchPlayer player;

			String username = null;
			Platform platform = null;
			if(args.size() == 1) {
				username = args.get(0);
				player = new OverwatchPlayer(username);
			} else {
				platform = this.getPlatform(args.get(0));
				username = args.get(1);
				player = new OverwatchPlayer(username, platform);
			}

			return context.getAuthorAvatarUrl()
					.map(avatarUrl -> EmbedUtils.getDefaultEmbed()
							.setAuthor("Overwatch Stats", player.getProfileURL(), avatarUrl)
							.setThumbnail(player.getIconUrl())
							.setDescription(String.format("Stats for user **%s**", player.getName()))
							.addField("Level", Integer.toString(player.getLevel()), true)
							.addField("Competitive rank", Integer.toString(player.getRank()), true)
							.addField("Wins", Integer.toString(player.getWins()), true)
							.addField("Game time", player.getTimePlayed(), true)
							.addField("Top hero (Time played)", this.getTopThreeHeroes(player.getList(TopHeroesStats.TIME_PLAYED)), true)
							.addField("Top hero (Eliminations per life)", this.getTopThreeHeroes(player.getList(TopHeroesStats.ELIMINATIONS_PER_LIFE)), true))
					.flatMap(loadingMsg::send)
					.then();

		} catch (OverwatchException err) {
			return context.getAuthorName()
					.flatMap(username -> loadingMsg.send(String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) %s", username, err.getMessage())))
					.then();
		} catch (IOException err) {
			loadingMsg.stopTyping();
			throw Exceptions.propagate(err);
		}
	}

	private String getTopThreeHeroes(List<HeroDesc> heroesList) {
		return FormatUtils.numberedList(3, heroesList.size(), count -> {
			final HeroDesc hero = heroesList.get(count - 1);
			return String.format("**%s**. %s (%s)", count, hero.getName(), hero.getDesc());
		});
	}

	private Platform getPlatform(String str) {
		Platform platform = Utils.getEnum(Platform.class, str.toUpperCase());
		if(platform == null) {
			throw new CommandException(String.format("`%s` is not a valid Platform. %s",
					str, FormatUtils.formatOptions(Platform.class)));
		}
		return platform;
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show player's stats for Overwatch.")
				.addArg("platform", String.format("user's platform (%s)",
						FormatUtils.format(Platform.values(), platform -> platform.toString().toLowerCase(), ", ")), true)
				.addArg("battletag#0000", false)
				.addField("Info", "**platform** is automatically detected if nothing is specified.", false)
				.build();
	}

}
