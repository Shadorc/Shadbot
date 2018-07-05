package me.shadorc.shadbot.command.fun;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.api.pandorabots.ChatBotResponse;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.FUN, names = { "chat" })
public class ChatCmd extends AbstractCommand {

	private static final List<String> BOTS_ID = List.of("efc39100ce34d038", "b0dafd24ee35a477", "ea373c261e3458c6", "b0a6a41a5e345c23");
	private static final int MAX_ERROR_COUNT = 10;

	private static final ConcurrentHashMap<Snowflake, String> CHANNELS_CUSTID = new ConcurrentHashMap<>();
	private static final AtomicInteger ERROR_COUNT = new AtomicInteger();

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		for(String botId : BOTS_ID) {
			try {
				String response = this.talk(context.getChannelId(), botId, arg);
				ERROR_COUNT.set(0);
				return BotUtils.sendMessage(Emoji.SPEECH + " " + response, context.getChannel()).then();
			} catch (JSONException | IOException err) {
				LogUtils.infof("{%s} %s is not reachable, trying another one.", this.getClass().getSimpleName(), botId);
			}
		}

		if(ERROR_COUNT.incrementAndGet() >= MAX_ERROR_COUNT) {
			LogUtils.error(context.getClient(),
					String.format("{%s} No artificial intelligence responds (Error count: %d).",
							this.getClass().getSimpleName(), ERROR_COUNT.get()));
		}

		return BotUtils.sendMessage(Emoji.SLEEPING + " Sorry, A.L.I.C.E. seems to be AFK, she'll probably come back later.",
				context.getChannel()).then();
	}

	private String talk(Snowflake channelId, String botId, String input) throws UnsupportedEncodingException, IOException {
		final String url = String.format("https://www.pandorabots.com/pandora/talk-xml?botid=%s&input=%s&custid=%s",
				botId, NetUtils.encode(input), CHANNELS_CUSTID.getOrDefault(channelId, ""));
		JSONObject resultObj = XML.toJSONObject(NetUtils.getDoc(url).html()).getJSONObject("result");
		ChatBotResponse chat = Utils.MAPPER.readValue(resultObj.toString(), ChatBotResponse.class);
		CHANNELS_CUSTID.put(channelId, chat.getCustId());
		return chat.getResponse();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
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
