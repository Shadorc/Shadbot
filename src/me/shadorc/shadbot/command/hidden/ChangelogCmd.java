package me.shadorc.shadbot.command.hidden;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.LoadingMessage;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

@RateLimited
@Command(category = CommandCategory.HIDDEN, names = { "changelog" })
public class ChangelogCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {

		LoadingMessage loadingMsg = new LoadingMessage("Loading changelog...", context.getChannel());
		loadingMsg.send();

		try {
			String url = String.format("https://api.github.com/repos/Shadorc/Shadbot/commits?access_token=%s", APIKeys.get(APIKey.GITHUB_TOKEN));
			JSONArray commitsArray = new JSONArray(NetUtils.getBody(url));

			StringBuilder desc = new StringBuilder();
			for(int i = 0; i < commitsArray.length(); i++) {
				JSONObject commitObj = commitsArray.getJSONObject(i).getJSONObject("commit");
				String message = commitObj.getString("message");

				if("Bump version".equals(message)) {
					break;
				}

				desc.append(String.format("\\* %s%n", message));
			}

			EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
					.withAuthorName(String.format("Shadbot Changelog [Version %s]", Shadbot.VERSION))
					.withAuthorUrl("https://github.com/Shadorc/Shadbot/commits/master")
					.withThumbnail("http://www.plusdoption.com/lib/img/all/github-logo.png")
					.withDescription(desc.toString());

			BotUtils.sendMessage(embed.build(), context.getChannel());

		} catch (JSONException | IOException err) {
			Utils.handle("getting changelog", context, err);
		}

		loadingMsg.delete();

	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show the changelog for the last update.")
				.build();
	}

}
