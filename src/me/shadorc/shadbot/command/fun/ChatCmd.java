package me.shadorc.shadbot.command.fun;

import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.api.pandorabots.ChatBotResponse;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import org.json.JSONObject;
import org.json.XML;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ChatCmd extends BaseCmd {

	private static final Map<String, String> BOTS = Map.of("Marvin", "efc39100ce34d038", "Chomsky", "b0dafd24ee35a477",
			"R.I.V.K.A", "ea373c261e3458c6", "Lisa", "b0a6a41a5e345c23");
	private static final int MAX_ERROR_COUNT = 10;
	private static final int MAX_CHARACTERS = 250;

	private final ConcurrentHashMap<Snowflake, String> channelsCustid;
	private final AtomicInteger errorCount;

	public ChatCmd() {
		super(CommandCategory.FUN, List.of("chat"));
		this.setDefaultRateLimiter();

		this.channelsCustid = new ConcurrentHashMap<>();
		this.errorCount = new AtomicInteger(0);
	}

	@Override
	public Mono<Void> execute(Context context) {
		final String arg = context.requireArg();

		if(arg.length() > MAX_CHARACTERS) {
			return Mono.error(new CommandException(String.format("The message must not exceed **%d characters**.",
					MAX_CHARACTERS)));
		}

		for(final Entry<String, String> bot : BOTS.entrySet()) {
			try {
				final String response = this.talk(context.getChannelId(), bot.getValue(), arg);
				this.errorCount.set(0);
				return context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.SPEECH + " **%s**: %s", bot.getKey(), response), channel))
						.then();
			} catch (final IOException err) {
				LogUtils.info("{%s} %s is not reachable, trying another one.", this.getClass().getSimpleName(), bot.getKey());
			}
		}

		if(this.errorCount.incrementAndGet() >= MAX_ERROR_COUNT) {
			LogUtils.error(context.getClient(),
					String.format("{%s} No artificial intelligence responds (Error count: %d).",
							this.getClass().getSimpleName(), this.errorCount.get()));
		}

		return context.getChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(
						String.format(Emoji.SLEEPING + " (**%s**) Sorry, A.L.I.C.E. seems to be AFK, she'll probably come back later.",
								context.getUsername()), channel))
				.then();
	}

	private String talk(Snowflake channelId, String botId, String input) throws IOException {
		final String url = String.format("https://www.pandorabots.com/pandora/talk-xml?botid=%s&input=%s&custid=%s",
				botId, NetUtils.encode(input), this.channelsCustid.getOrDefault(channelId, ""));
		final JSONObject resultObj = XML.toJSONObject(NetUtils.getDoc(url).html()).getJSONObject("result");
		final ChatBotResponse chat = Utils.MAPPER.readValue(resultObj.toString(), ChatBotResponse.class);
		this.channelsCustid.put(channelId, chat.getCustId());
		return chat.getResponse();
	}

	@Override
	public Consumer<EmbedCreateSpec> getHelp(Context context) {
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
