package me.shadorc.shadbot.command.gamestats;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONException;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.api.diablo.HeroResponse;
import me.shadorc.shadbot.api.diablo.ProfileHeroResponse;
import me.shadorc.shadbot.api.diablo.ProfileResponse;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.GAMESTATS, names = { "diablo" }, alias = "d3")
public class DiabloCmd extends AbstractCommand {

	private enum Region {
		EU, US, TW, KR;
	}

	@Override
	public Mono<Void> execute(Context context) {
		List<String> args = context.requireArgs(2);

		final Region region = Utils.getValueOrNull(Region.class, args.get(0));
		if(region == null) {
			throw new CommandException(String.format("`%s` is not a valid Region. %s",
					args.get(0), FormatUtils.formatOptions(Region.class)));
		}

		final String battletag = args.get(1).replaceAll("#", "-");

		LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			final URL url = new URL(String.format("https://%s.api.battle.net/d3/profile/%s/?locale=en_GB&apikey=%s",
					region, NetUtils.encode(battletag), APIKeys.get(APIKey.BLIZZARD_API_KEY)));

			ProfileResponse profile = Utils.MAPPER.readValue(url, ProfileResponse.class);

			if("NOTFOUND".equals(profile.getCode())) {
				loadingMsg.send(Emoji.MAGNIFYING_GLASS + " This user doesn't play Diablo 3 or doesn't exist.");
				return Mono.empty();
			}

			List<HeroResponse> heroResponses = new ArrayList<>();
			for(ProfileHeroResponse profileHero : profile.getHeroes()) {
				final URL heroUrl = new URL(String.format("https://%s.api.battle.net/d3/profile/%s/hero/%d?locale=en_GB&apikey=%s",
						region, NetUtils.encode(battletag), profileHero.getId(), APIKeys.get(APIKey.BLIZZARD_API_KEY)));

				HeroResponse hero = Utils.MAPPER.readValue(heroUrl, HeroResponse.class);
				if(hero.getCode() == null) {
					heroResponses.add(hero);
				}
			}

			// Sort heroes by ascending damage
			heroResponses.sort((hero1, hero2) -> Double.compare(hero1.getStats().getDamage(), hero2.getStats().getDamage()));
			Collections.reverse(heroResponses);

			return context.getAuthorAvatarUrl()
					.map(avatarUrl -> {
						return EmbedUtils.getDefaultEmbed()
								.setAuthor("Diablo 3 Stats", null, avatarUrl)
								.setThumbnail("http://osx.wdfiles.com/local--files/icon:d3/D3.png")
								.setDescription(String.format("Stats for **%s** (Guild: **%s**)"
										+ "%n%nParangon level: **%s** (*Normal*) / **%s** (*Hardcore*)"
										+ "%nSeason Parangon level: **%s** (*Normal*) / **%s** (*Hardcore*)",
										profile.getBattleTag(), profile.getGuildName(),
										profile.getParagonLevel(), profile.getParagonLevelSeasonHardcore(),
										profile.getParagonLevelSeason(), profile.getParagonLevelSeasonHardcore()))
								.addField("Heroes", FormatUtils.format(heroResponses,
										hero -> String.format("**%s** (*%s*)", hero.getName(), hero.getClassName()), "\n"), true)
								.addField("Damage", FormatUtils.format(heroResponses,
										hero -> String.format("%s DPS", FormatUtils.formatNum(hero.getStats().getDamage())), "\n"), true);
					})
					.doOnSuccess(embed -> loadingMsg.send(embed))
					.then();

		} catch (FileNotFoundException err) {
			loadingMsg.send(Emoji.MAGNIFYING_GLASS + " This user doesn't play Diablo 3 or doesn't exist.");
		} catch (JSONException | IOException err) {
			loadingMsg.stopTyping();
			throw Exceptions.propagate(err);
		}

		return Mono.empty();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show player's stats for Diablo 3.")
				.addArg("region", String.format("user's region (%s)", FormatUtils.format(Region.values(), region -> region.toString().toLowerCase(), ", ")), false)
				.addArg("battletag#0000", false)
				.setExample(String.format("`%s%s eu Shadbot#1758`", context.getPrefix(), this.getName()))
				.build();
	}

}
