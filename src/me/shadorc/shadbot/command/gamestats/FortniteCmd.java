package me.shadorc.shadbot.command.gamestats;

import java.net.URL;
import java.util.List;
import java.util.function.Consumer;

import org.apache.http.HttpStatus;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.api.gamestats.fortnite.FortniteResponse;
import me.shadorc.shadbot.api.gamestats.fortnite.Stats;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.credential.Credential;
import me.shadorc.shadbot.data.credential.Credentials;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
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

			final Response response = NetUtils.getResponse(url.toString())
					.header("TRN-Api-Key", Credentials.get(Credential.FORTNITE_API_KEY));

			if(response.statusCode() != 200) {
				loadingMsg.stopTyping();
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

			final Consumer<EmbedCreateSpec> embedConsumer = EmbedUtils.getDefaultEmbed()
					.andThen(embed -> embed.setAuthor("Fortnite Stats",
							String.format("https://fortnitetracker.com/profile/%s/%s",
									StringUtils.toLowerCase(platform), encodedNickname),
							context.getAvatarUrl())
							.setThumbnail("https://orig00.deviantart.net/9517/f/2017/261/9/f/fortnite___icon_by_blagoicons-dbnu8a0.png")
							.setDescription(description));

			return loadingMsg.send(embedConsumer).then();

		} catch (final Exception err) {
			loadingMsg.stopTyping();
			throw Exceptions.propagate(err);
		}
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show player's stats for Fortnite.")
				.addArg("platform", String.format("user's platform (%s)", FormatUtils.format(Platform.class, ", ")),
						false)
				.addArg("epic-nickname", false)
				.setExample(String.format("`%s%s pc Shadbot`", context.getPrefix(), this.getName()))
				.build();
	}

}
