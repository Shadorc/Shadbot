package me.shadorc.shadbot.command.fun;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.jsoup.nodes.Document;

import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;

@RateLimited
@Command(category = CommandCategory.FUN, names = { "chat" })
public class ChatCmd extends AbstractCommand {

	private static final List<String> BOTS_ID = List.of("efc39100ce34d038", "b0dafd24ee35a477", "ea373c261e3458c6", "b0a6a41a5e345c23");
	private static final int MAX_ERROR_COUNT = 10;

	private static final ConcurrentHashMap<Snowflake, String> CHANNELS_CUSTID = new ConcurrentHashMap<>();

	private static int errorCount;

	@Override
	public void execute(Context context) throws MissingArgumentException {
		context.requireArg();

		String response;
		for(String botId : BOTS_ID) {
			try {
				response = this.talk(context.getChannelId(), botId, context.getArg().get());
				BotUtils.sendMessage(Emoji.SPEECH + " " + response, context.getChannel());
				errorCount = 0;
				return;
			} catch (JSONException | IOException err) {
				LogUtils.infof("{%s} %s is not reachable, trying another one.", this.getClass().getSimpleName(), botId);
			}
		}

		BotUtils.sendMessage(Emoji.SLEEPING + " Sorry, A.L.I.C.E. seems to be AFK, she'll probably come back later.", context.getChannel());

		errorCount++;
		if(errorCount >= MAX_ERROR_COUNT) {
			LogUtils.error(String.format("No artificial intelligence is responding (Error count: %d).", errorCount));
		}
	}

	private String talk(Snowflake channelId, String botId, String input) throws UnsupportedEncodingException, IOException {
		String url = String.format("https://www.pandorabots.com/pandora/talk-xml?botid=%s&input=%s&custid=%s",
				botId, NetUtils.encode(input), CHANNELS_CUSTID.getOrDefault(channelId, ""));
		Document doc = NetUtils.getDoc(url);
		JSONObject mainObj = XML.toJSONObject(doc.html());
		JSONObject resultObj = mainObj.getJSONObject("result");
		CHANNELS_CUSTID.put(channelId, resultObj.getString("custid"));
		return StringUtils.normalizeSpace(resultObj.getString("that").replace("<br>", "\n"));
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Chat with an artificial intelligence.")
				.addArg("message", false)
				.setSource("https://pandorabots.com/"
						+ "\n**Marvin** (ID: efc39100ce34d038)"
						+ "\n**Chomsky** (ID: b0dafd24ee35a477)"
						+ "\n**R.I.V.K.A** (ID: ea373c261e3458c6)"
						+ "\n**Lisa** (ID: b0a6a41a5e345c23)")
				.build();
	}
}
