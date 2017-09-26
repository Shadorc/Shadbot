package me.shadorc.discordbot.command.utils;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.temporal.ChronoUnit;

import org.json.JSONObject;
import org.json.XML;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class LyricsCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public LyricsCmd() {
		super(CommandCategory.UTILS, Role.USER, "lyrics");
		this.rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		String[] args = context.getArg().replaceAll("<|>", "").split("-", 2);
		if(args.length != 2) {
			throw new MissingArgumentException();
		}

		String artist = args[0].trim();
		String title = args[1].trim();

		try {
			String xmlString = NetUtils.getDoc("http://api.chartlyrics.com/apiv1.asmx/SearchLyricDirect?"
					+ "artist=" + URLEncoder.encode(artist, "UTF-8")
					+ "&song=" + URLEncoder.encode(title, "UTF-8")).toString();
			JSONObject resultObj = XML.toJSONObject(xmlString).getJSONObject("GetLyricResult");

			if(resultObj.getInt("LyricId") == 0) {
				BotUtils.send(Emoji.MAGNIFYING_GLASS + " No lyrics found for \"" + context.getArg() + "\"", context.getChannel());
				return;
			}

			EmbedBuilder embed = Utils.getDefaultEmbed()
					.setLenient(true)
					.withAuthorName("Lyrics")
					.withUrl(resultObj.getString("LyricUrl"))
					.withThumbnail(resultObj.getString("LyricCovertArtUrl"))
					.appendField("Song", resultObj.getString("LyricArtist") + " - " + resultObj.getString("LyricSong"), false)
					.appendField("Lyrics", resultObj.getString("LyricUrl"), false);
			BotUtils.send(embed.build(), context.getChannel());

		} catch (IOException err) {
			LogUtils.error("Something went wrong while getting lyrics... Please, try again later.", err, context);
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show lyrics for a song.**")
				.appendField("Usage", "`" + context.getPrefix() + "lyrics <artist> - <title>`", false);
		BotUtils.send(builder.build(), context.getChannel());
	}
}
