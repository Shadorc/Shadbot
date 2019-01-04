package me.shadorc.shadbot.command.gamestats;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.api.gamestats.overwatch.profile.ProfileResponse;
import me.shadorc.shadbot.api.gamestats.overwatch.stats.Quickplay;
import me.shadorc.shadbot.api.gamestats.overwatch.stats.StatsResponse;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

@RateLimited
@Command(category = CommandCategory.GAMESTATS, names = { "overwatch" }, alias = "ow")
public class OverwatchCmd extends AbstractCommand {

	private enum Platform {
		PC, PSN, XBL;
	}

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(1, 2);

		final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			final Tuple3<Platform, ProfileResponse, StatsResponse> response =
					args.size() == 1 ? this.getResponse(args.get(0)) : this.getResponse(args.get(0), args.get(1));

			if(response == null) {
				return loadingMsg.send(
						String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) Overwatch player not found.", context.getUsername()))
						.then();
			}

			final Platform platform = response.getT1();
			final ProfileResponse profile = response.getT2();
			final Quickplay topHeroes = response.getT3().getStats().getTopHeroes().getQuickplay();

			return context.getAvatarUrl()
					.map(avatarUrl -> EmbedUtils.getDefaultEmbed()
							.setAuthor("Overwatch Stats (Quickplay)", String.format("https://playoverwatch.com/en-gb/career/%s/%s",
									StringUtils.toLowerCase(platform), profile.getUsername()), avatarUrl)
							.setThumbnail(profile.getPortrait())
							.setDescription(String.format("Stats for user **%s**", profile.getUsername()))
							.addField("Level", profile.getLevel(), true)
							.addField("Competitive rank", profile.getRank(), true)
							.addField("Games won", profile.getGames().getQuickplayWon(), true)
							.addField("Time played", profile.getQuickplayPlaytime(), true)
							.addField("Top hero (Time played)", topHeroes.getPlayed(), true)
							.addField("Top hero (Eliminations per life)", topHeroes.getEliminationsPerLife(), true))
					.flatMap(loadingMsg::send)
					.then();

		} catch (final IOException err) {
			loadingMsg.stopTyping();
			throw Exceptions.propagate(err);
		}
	}

	private Tuple3<Platform, ProfileResponse, StatsResponse> getResponse(String battletag) throws IOException {
		for(final Platform platform : Platform.values()) {
			final Tuple3<Platform, ProfileResponse, StatsResponse> response = this.getResponse(platform.toString(), battletag);
			if(response != null) {
				return response;
			}
		}

		throw new CommandException(String.format("Platform not found. Try again specifying it. %s",
				FormatUtils.options(Platform.class)));
	}

	private Tuple3<Platform, ProfileResponse, StatsResponse> getResponse(String platformStr, String battletag) throws IOException {
		final String username = battletag.replace("#", "-");
		final Platform platform = Utils.getEnum(Platform.class, platformStr);
		if(platform == null) {
			throw new CommandException(String.format("`%s` is not a valid Platform. %s",
					platformStr, FormatUtils.options(Platform.class)));
		}

		final ProfileResponse profile = Utils.MAPPER.readValue(this.getUrl("profile", platform, username), ProfileResponse.class);
		if(profile.getUsername() == null) {
			return null;
		}
		final StatsResponse stats = Utils.MAPPER.readValue(this.getUrl("stats", platform, username), StatsResponse.class);
		if(stats.getStats() == null) {
			return null;
		}
		return Tuples.of(platform, profile, stats);
	}

	private URL getUrl(String endpoint, Platform platform, String username) throws MalformedURLException {
		return new URL(String.format("http://ow-api.herokuapp.com/%s/%s/global/%s",
				endpoint, StringUtils.toLowerCase(platform), username));
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show player's stats for Overwatch.")
				.addArg("platform", String.format("user's platform (%s)", FormatUtils.format(Platform.class, ", ")), true)
				.addArg("username", "case sensitive", false)
				.addField("Info", "**platform** is automatically detected if nothing is specified.", false)
				.setExample(String.format("%s%s pc Shadorc#2503", context.getPrefix(), context.getCommandName()))
				.build();
	}

}
