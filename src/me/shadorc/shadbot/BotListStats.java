package me.shadorc.shadbot;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONObject;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.data.credential.Credential;
import me.shadorc.shadbot.data.credential.Credentials;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

public class BotListStats {

	private final Snowflake selfId;
	private final Disposable task;

	public BotListStats(Snowflake selfId) {
		this.selfId = selfId;
		this.task = Flux.interval(Duration.ofHours(2), Duration.ofHours(2))
				.flatMap(ignored -> this.postStats())
				.onErrorContinue((err, obj) -> LogUtils.error(err, "An error occurred while posting statistics."))
				.subscribe();
	}

	private Mono<Void> postStats() {
		if(Config.IS_SNAPSHOT) {
			return Mono.empty();
		}

		return Flux.fromIterable(Shadbot.getClients())
				.flatMap(client -> Mono.zip(Mono.just(client.getConfig().getShardIndex()), client.getGuilds().count()))
				.collectMap(Tuple2::getT1, Tuple2::getT2)
				.doOnSuccess(shardsInfo -> {
					LogUtils.info("Posting statistics...");
					this.postOnBotListDotSpace(shardsInfo);
					this.postOnDiscordBotsDotGg(shardsInfo);
					this.postOnDiscordBotListDotCom(shardsInfo);
					this.postOnDiscordBotsDotOrg(shardsInfo);
					this.postOnDivineDiscordBotsDotCom(shardsInfo);
					this.postOnBotsOndiscordXyz(shardsInfo);
					LogUtils.info("Statistics posted.");
				})
				.then();
	}

	/**
	 * WebSite: https://discordbots.org/ <br>
	 * Documentation: https://discordbots.org/api/docs#bots
	 */
	private void postOnDiscordBotsDotOrg(Map<Integer, Long> shardsInfo) {
		for(Entry<Integer, Long> entry : shardsInfo.entrySet()) {
			final JSONObject content = new JSONObject()
					.put("shard_id", entry.getKey())
					.put("shard_count", shardsInfo.size())
					.put("server_count", entry.getValue());
			final String url = String.format("https://discordbots.org/api/bots/%d/stats", this.selfId);

			try {
				this.post(url, Credentials.get(Credential.DISCORD_BOTS_DOT_ORG_TOKEN), content);
			} catch (final IOException err) {
				LogUtils.error(err, "An error occurred while posting statistics on discordbots.org");
			}
		}
	}

	/**
	 * WebSite: https://discord.bots.gg/ <br>
	 * Documentation: https://discord.bots.gg/docs/endpoints
	 */
	private void postOnDiscordBotsDotGg(Map<Integer, Long> shardsInfo) {
		for(Entry<Integer, Long> entry : shardsInfo.entrySet()) {
			final JSONObject content = new JSONObject()
					.put("shardId", entry.getKey())
					.put("shardCount", shardsInfo.size())
					.put("guildCount", entry.getValue());
			final String url = String.format("https://discord.bots.gg/api/bots/%d/stats", this.selfId);

			try {
				this.post(url, Credentials.get(Credential.DISCORD_BOTS_DOT_GG_TOKEN), content);
			} catch (final IOException err) {
				LogUtils.error(err, "An error occurred while posting statistics on discord.bots.gg");
			}
		}
	}

	/**
	 * WebSite: https://discordbotlist.com/ <br>
	 * Documentation: https://discordbotlist.com/api-docs
	 */
	private void postOnDiscordBotListDotCom(Map<Integer, Long> shardsInfo) {
		for(Entry<Integer, Long> entry : shardsInfo.entrySet()) {
			final JSONObject content = new JSONObject()
					.put("shard_id", entry.getKey())
					.put("guilds ", entry.getValue());
			final String url = String.format("https://discordbotlist.com/api/bots/%d/stats", this.selfId);

			try {
				this.post(url, String.format("Bot %s", Credentials.get(Credential.DISCORD_BOT_LIST_DOT_COM_TOKEN)), content);
			} catch (final IOException err) {
				LogUtils.error(err, "An error occurred while posting statistics on discordbotlist.com");
			}
		}
	}

	/**
	 * WebSite: https://divinediscordbots.com/ <br>
	 * Documentation: https://divinediscordbots.com/api
	 */
	private void postOnDivineDiscordBotsDotCom(Map<Integer, Long> shardsInfo) {
		final long guildCount = shardsInfo.values().stream().mapToLong(Long::longValue).sum();
		final JSONObject content = new JSONObject()
				.put("server_count", guildCount);
		final String url = String.format("https://divinediscordbots.com/bots/%d/stats", this.selfId);

		try {
			this.post(url, Credentials.get(Credential.DIVINE_DISCORD_BOTS_DOT_COM_TOKEN), content);
		} catch (final IOException err) {
			LogUtils.error(err, "An error occurred while posting statistics on divinediscordbots.com");
		}
	}

	/**
	 * WebSite: https://botlist.space/ <br>
	 * Documentation: https://botlist.space/documentation
	 */
	private void postOnBotListDotSpace(Map<Integer, Long> shardsInfo) {
		final long guildCount = shardsInfo.values().stream().mapToLong(Long::longValue).sum();
		final JSONObject content = new JSONObject()
				.put("server_count", guildCount);
		final String url = String.format("https://botlist.space/bots/%d/stats", this.selfId);

		try {
			this.post(url, Credentials.get(Credential.BOT_LIST_DOT_SPACE), content);
		} catch (final IOException err) {
			LogUtils.error(err, "An error occurred while posting statistics on botlist.space");
		}
	}

	/**
	 * WebSite: https://bots.ondiscord.xyz/ <br>
	 * Documentation: https://bots.ondiscord.xyz/info/api
	 *
	 * @param shardsInfo
	 */
	private void postOnBotsOndiscordXyz(Map<Integer, Long> shardsInfo) {
		final long guildCount = shardsInfo.values().stream().mapToLong(Long::longValue).sum();
		final JSONObject content = new JSONObject()
				.put("guildCount", guildCount);
		final String url = String.format("https://bots.ondiscord.xyz/bot-api/bots/%d/guilds", this.selfId);

		try {
			this.post(url, Credentials.get(Credential.BOTS_ONDISCORD_DOT_XYZ), content);
		} catch (final IOException err) {
			LogUtils.error(err, "An error occurred while posting statistics on bots.ondiscord.xyz");
		}
	}

	private void post(String url, String authorization, JSONObject content) throws IOException {
		Jsoup.connect(url)
				.method(Method.POST)
				.ignoreContentType(true)
				.headers(Map.of("Content-Type", "application/json", "Authorization", authorization))
				.requestBody(content.toString())
				.post();
	}

	public void stop() {
		this.task.dispose();
	}

}
