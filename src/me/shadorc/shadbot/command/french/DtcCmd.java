package me.shadorc.shadbot.command.french;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.MathUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.LoadingMessage;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

@RateLimited
@Command(category = CommandCategory.FRENCH, names = { "dtc" })
public class DtcCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		LoadingMessage loadingMsg = new LoadingMessage("Loading quote...", context.getChannel());
		loadingMsg.send();

		try {
			String url = String.format("http://api.danstonchat.com/0.3/view/random?key=%s&format=json", APIKeys.get(APIKey.DTC_API_KEY));
			JSONArray arrayObj = new JSONArray(NetUtils.getBody(url));

			JSONObject quoteObj;
			String content;
			do {
				quoteObj = arrayObj.getJSONObject(MathUtils.rand(arrayObj.length()));
				content = quoteObj.getString("content");
				content = content.replace("*", "\\*");
			} while(content.length() > 1000);

			StringBuilder strBuilder = new StringBuilder();
			for(String line : content.split("\n")) {
				strBuilder.append('\n');
				if(line.contains(" ")) {
					strBuilder.append("**" + line.substring(0, line.indexOf(' ')) + "** " + line.substring(line.indexOf(' ') + 1));
				} else {
					strBuilder.append(line);
				}
			}

			EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
					.withAuthorName("Quote DansTonChat")
					.withUrl(String.format("https://danstonchat.com/%s.html", quoteObj.getString("id")))
					.withThumbnail("https://danstonchat.com/themes/danstonchat/images/logo2.png")
					.appendDescription(strBuilder.toString());
			loadingMsg.edit(embed.build());

		} catch (JSONException | IOException err) {
			ExceptionUtils.handle("getting a quote from DansTonChat.com", context, err);
		}
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show a random quote from DansTonChat.com")
				.setSource("https://danstonchat.com")
				.build();
	}
}
