package me.shadorc.shadbot.command.fun;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.FUN, names = { "chat" })
public class ChatCmd extends AbstractCommand {

	private static final Map<String, String> BOTS = Map.of("Marvin", "efc39100ce34d038", "Chomsky", "b0dafd24ee35a477",
			"R.I.V.K.A", "ea373c261e3458c6", "Lisa", "b0a6a41a5e345c23");
	private static final int MAX_ERROR_COUNT = 10;
	private static final int MAX_CHARACTERS = 250;

	private static final ConcurrentHashMap<Snowflake, String> CHANNELS_CUSTID = new ConcurrentHashMap<>();
	private static final AtomicInteger ERROR_COUNT = new AtomicInteger();

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		if(arg.length() > MAX_CHARACTERS) {
			throw new CommandException(String.format("The message must not exceed **%d characters**.", MAX_CHARACTERS));
		}
		
		for(final String botName : BOTS.keySet()) {
			try {
				final String response = this.talk(context.getChannelId(), BOTS.get(botName), arg);
				ERROR_COUNT.set(0);
				return context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.SPEECH + " **%s**: %s", botName, response), channel))
						.then();
			} catch (final IOException err) {
				LogUtils.info("{%s} %s is not reachable, trying another one.", this.getClass().getSimpleName(), botName);
			}
		}

		if(ERROR_COUNT.incrementAndGet() >= MAX_ERROR_COUNT) {
			LogUtils.error(context.getClient(),
					String.format("{%s} No artificial intelligence responds (Error count: %d).",
							this.getClass().getSimpleName(), ERROR_COUNT.get()));
		}

		return context.getChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(
						String.format(Emoji.SLEEPING + " (**%s**) Sorry, A.L.I.C.E. seems to be AFK, she'll probably come back later.",
								context.getUsername()), channel))
				.then();
	}

	private String talk(Snowflake channelId, String botId, String input) throws UnsupportedEncodingException, IOException {
		final String url = String.format("https://www.pandorabots.com/pandora/talk-xml?botid=%s&input=%s&custid=%s",
				botId, NetUtils.encode(input), CHANNELS_CUSTID.getOrDefault(channelId, ""));
		final JSONObject resultObj = XML.toJSONObject(NetUtils.getDoc(url).html()).getJSONObject("result");
		final ChatBotResponse chat = Utils.MAPPER.readValue(resultObj.toString(), ChatBotResponse.class);
		CHANNELS_CUSTID.put(channelId, chat.getCustId());
		return chat.getResponse();
	}

	@Override
	public Mono<Consumer<? super EmbedCreateSpec>> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Chat with an artificial intelligence.")
				.addArg("message", String.format("must not exceed %d characters", MAX_CHARACTERS), false)
				.setSource("https://www.pandorabots.com/"
						+ "\n**Marvin** (ID: efc39100ce34d038)"
						+ "\n**Chomsky** (ID: b0dafd24ee35a477)"
						+ "\n**R.I.V.K.A** (ID: ea373c261e3458c6)"
						+ "\n**Lisa** (ID: b0a6a41a5e345c23)")
				.build();
	}
}
