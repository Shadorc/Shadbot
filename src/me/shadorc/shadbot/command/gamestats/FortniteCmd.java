package me.shadorc.shadbot.command.gamestats;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.apache.http.HttpStatus;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.api.gamestats.fortnite.FortniteResponse;
import me.shadorc.shadbot.api.gamestats.fortnite.Stats;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.apikey.APIKey;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.GAMESTATS, names = { "fortnite" })
public class FortniteCmd extends AbstractCommand {

	private enum Platform {
		PC, XBL, PSN;
	}

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(2);

		final Platform platform = Utils.getEnum(Platform.class, args.get(0));
		if(platform == null) {
			throw new CommandException(String.format("`%s` is not a valid Platform. %s",
					args.get(0), FormatUtils.options(Platform.class)));
		}

		final String epicNickname = args.get(1);

		final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			final String encodedNickname = epicNickname.replace(" ", "%20");
			final URL url = new URL(String.format("https://api.fortnitetracker.com/v1/profile/%s/%s",
					StringUtils.toLowerCase(platform), encodedNickname));

			final Response response = Jsoup.connect(url.toString())
					.method(Method.GET)
					.ignoreContentType(true)
					.ignoreHttpErrors(true)
					.header("TRN-Api-Key", Shadbot.getAPIKeys().get(APIKey.FORTNITE_API_KEY))
					.execute();

			if(response.statusCode() != 200) {
				throw new HttpStatusException("Fortnite API did not return a valid status code.",
						HttpStatus.SC_SERVICE_UNAVAILABLE, url.toString());
			}

			final FortniteResponse fortnite = Utils.MAPPER.readValue(response.parse().body().html(), FortniteResponse.class);

			if(fortnite.getError().map("Player Not Found"::equals).orElse(false)) {
				return loadingMsg.send(
						String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) This user doesn't play Fortnite on this platform or doesn't exist.",
								context.getUsername()))
						.then();
			}

			final int length = 8;
			final String format = "%n%-" + (length + 5) + "s %-" + length + "s %-" + length + "s %-" + (length + 3) + "s";
			final Stats stats = fortnite.getStats();

			final String description = String.format("Stats for user **%s**%n", epicNickname)
					+ "```prolog"
					+ String.format(format, " ", "Solo", "Duo", "Squad")
					+ String.format(format, "Top 1", stats.getSoloStats().getTop1(), stats.getDuoStats().getTop1(), stats.getSquadStats().getTop1())
					+ String.format(format, "K/D season", stats.getSeasonSoloStats().getRatio(), stats.getSeasonDuoStats().getRatio(), stats.getSeasonSquadStats().getRatio())
					+ String.format(format, "K/D lifetime", stats.getSoloStats().getRatio(), stats.getDuoStats().getRatio(), stats.getSquadStats().getRatio())
					+ "```";

			return context.getAvatarUrl()
					.map(avatarUrl -> {
						return EmbedUtils.getDefaultEmbed()
								.setAuthor("Fortnite Stats",
										String.format("https://fortnitetracker.com/profile/%s/%s",
												StringUtils.toLowerCase(platform), encodedNickname),
										avatarUrl)
								.setThumbnail("https://orig00.deviantart.net/9517/f/2017/261/9/f/fortnite___icon_by_blagoicons-dbnu8a0.png")
								.setDescription(description);
					})
					.flatMap(loadingMsg::send)
					.then();

		} catch (IOException err) {
			loadingMsg.stopTyping();
			throw Exceptions.propagate(err);
		}
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show player's stats for Fortnite.")
				.addArg("platform", String.format("user's platform (%s)", FormatUtils.format(Platform.class, ", ")),
						false)
				.addArg("epic-nickname", false)
				.setExample(String.format("`%s%s pc Shadbot`", context.getPrefix(), this.getName()))
				.build();
	}

}
