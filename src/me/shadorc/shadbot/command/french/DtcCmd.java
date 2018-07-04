package me.shadorc.shadbot.command.french;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.api.dtc.QuoteResponse;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.FRENCH, names = { "dtc" })
public class DtcCmd extends AbstractCommand {

	@Override
	public Mono<Void> execute(Context context) {
		LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		return context.getAuthorAvatarUrl()
				.map(avatarUrl -> {
					try {
						final String url = String.format("http://api.danstonchat.com/0.3/view/random?key=%s&format=json",
								APIKeys.get(APIKey.DTC_API_KEY));
						final JSONArray quoteObjs = new JSONArray(NetUtils.getJSON(url));

						for(JSONObject quoteObj : Utils.toList(quoteObjs, JSONObject.class)) {
							QuoteResponse quote = Utils.MAPPER.readValue(quoteObj.toString(), QuoteResponse.class);
							if(quote.getContent().length() < 1000) {
								final String content = quote.getContent().replace("*", "\\*");

								return loadingMsg.send(EmbedUtils.getDefaultEmbed()
										.setAuthor("Quote DansTonChat",
												String.format("https://danstonchat.com/%s.html", quote.getId()),
												avatarUrl)
										.setThumbnail("https://danstonchat.com/themes/danstonchat/images/logo2.png")
										.setDescription(FormatUtils.format(content.split("\n"), this::format, "\n")));
							}
						}

						LogUtils.warn(context.getClient(), String.format("{%s} No quotes were found.", this.getClass().getSimpleName()));
						return loadingMsg.send(EmbedUtils.getDefaultEmbed()
								.setAuthor("Quote DansTonChat", null, avatarUrl)
								.setThumbnail("https://danstonchat.com/themes/danstonchat/images/logo2.png")
								.setDescription("Sorry, no quotes were found."));

					} catch (JSONException | IOException err) {
						loadingMsg.stopTyping();
						throw Exceptions.propagate(err);
					}
				})
				.then();
	}

	private String format(String line) {
		// Set the user name as bold
		if(line.contains(" ")) {
			int index = line.indexOf(' ');
			return "**" + line.substring(0, index) + "** " + line.substring(index + 1);
		}
		return line;
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show a random quote from DansTonChat.com")
				.setSource("https://danstonchat.com")
				.build();
	}
}
