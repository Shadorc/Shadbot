package me.shadorc.shadbot;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import org.json.JSONObject;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;

import discord4j.core.DiscordClient;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.data.apikey.APIKey;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.Disposable;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class BotListStats {

	private final Disposable task;

	public BotListStats() {
		this.task = Flux.interval(Duration.ofHours(2), Duration.ofHours(2))
				.flatMap(ignored -> postStats())
				.onErrorContinue((err, obj) -> LogUtils.error(err, "An error occurred while posting statistics."))
				.subscribe();
	}

	private Mono<Void> postStats() {
		if(Config.IS_SNAPSHOT) {
			return Mono.empty();
		}
		return Mono.fromRunnable(() -> LogUtils.info("Posting statistics..."))
				.then(this.postOnBotListDotSpace())
				.then(this.postOnBotsDiscordDotPw())
				.then(this.postOnDiscordBotListDotCom())
				.then(this.postOnDiscordBotsDotOrg())
				.then(this.postOnDivineDiscordBotsDotCom())
				.then(Mono.fromRunnable(() -> LogUtils.info("Statistics posted.")));
	}

	/**
	 * WebSite: https://discordbots.org/ <br>
	 * Documentation: https://discordbots.org/api/docs#bots
	 */
	private Mono<Void> postOnDiscordBotsDotOrg() {
		return Flux.fromIterable(Shadbot.getClients())
				.flatMap(client -> client.getGuilds()
						.count()
						.doOnSuccess(guildsCount -> {
							final Long selfId = client.getSelfId().map(Snowflake::asLong).orElse(0L);
							final JSONObject content = new JSONObject()
									.put("shard_id", client.getConfig().getShardIndex())
									.put("shard_count", client.getConfig().getShardCount())
									.put("server_count", guildsCount);
							final String url = String.format("https://discordbots.org/api/bots/%d/stats", selfId);

							try {
								this.post(url, Shadbot.getAPIKeys().get(APIKey.DISCORD_BOTS_DOT_ORG_TOKEN), content);
							} catch (IOException err) {
								Exceptions.propagate(err);
							}
						}))
				.doOnError(err -> LogUtils.error(err, "An error occurred while posting statistics on discordbots.org"))
				.then();
	}

	/**
	 * WebSite: https://bots.discord.pw/ <br>
	 * Documentation: TODO
	 */
	private Mono<Void> postOnBotsDiscordDotPw() {
		return Flux.fromIterable(Shadbot.getClients())
				.flatMap(client -> client.getGuilds()
						.count()
						.doOnSuccess(guildsCount -> {
							final Long selfId = client.getSelfId().map(Snowflake::asLong).orElse(0L);
							final JSONObject content = new JSONObject()
									.put("shard_id", client.getConfig().getShardIndex())
									.put("shard_count", client.getConfig().getShardCount())
									.put("server_count", guildsCount);
							final String url = String.format("https://bots.discord.pw/api/bots/%d/stats", selfId);

							try {
								this.post(url, Shadbot.getAPIKeys().get(APIKey.BOTS_DISCORD_DOT_PW_TOKEN), content);
							} catch (IOException err) {
								Exceptions.propagate(err);
							}
						}))
				.doOnError(err -> LogUtils.error(err, "An error occurred while posting statistics on bots.discord.pw"))
				.then();
	}

	/**
	 * WebSite: https://discordbotlist.com/ <br>
	 * Documentation: https://discordbotlist.com/api-docs
	 */
	private Mono<Void> postOnDiscordBotListDotCom() {
		return Flux.fromIterable(Shadbot.getClients())
				.flatMap(client -> client.getGuilds()
						.count()
						.doOnSuccess(guildsCount -> {
							final Long selfId = client.getSelfId().map(Snowflake::asLong).orElse(0L);
							final JSONObject content = new JSONObject()
									.put("shard_id", client.getConfig().getShardIndex())
									.put("guilds ", guildsCount);
							final String url = String.format("https://discordbotlist.com/api/bots/%d/stats", selfId);

							try {
								this.post(url, String.format("Bot %s", Shadbot.getAPIKeys().get(APIKey.DISCORD_BOT_LIST_DOT_COM_TOKEN)), content);
							} catch (IOException err) {
								Exceptions.propagate(err);
							}
						}))
				.doOnError(err -> LogUtils.error(err, "An error occurred while posting statistics on discordbotlist.com"))
				.then();
	}

	/**
	 * WebSite: https://divinediscordbots.com/ <br>
	 * Documentation: https://divinediscordbots.com/api
	 */
	private Mono<Void> postOnDivineDiscordBotsDotCom() {
		return Flux.fromIterable(Shadbot.getClients())
				.flatMap(DiscordClient::getGuilds)
				.count()
				.doOnSuccess(guildsCount -> {
					final Long selfId = Shadbot.getClients().get(0).getSelfId().map(Snowflake::asLong).orElse(0L);
					final JSONObject content = new JSONObject()
							.put("server_count", guildsCount);
					final String url = String.format("https://divinediscordbots.com/bots/%d/stats", selfId);

					try {
						this.post(url, Shadbot.getAPIKeys().get(APIKey.DIVINE_DISCORD_BOTS_DOT_COM_TOKEN), content);
					} catch (IOException err) {
						Exceptions.propagate(err);
					}
				})
				.doOnError(err -> LogUtils.error(err, "An error occurred while posting statistics on divinediscordbots.com"))
				.then();
	}

	/**
	 * WebSite: https://botlist.space/ <br>
	 * Documentation: https://botlist.space/documentation
	 */
	private Mono<Void> postOnBotListDotSpace() {
		return Flux.fromIterable(Shadbot.getClients())
				.flatMap(DiscordClient::getGuilds)
				.count()
				.doOnSuccess(guildsCount -> {
					final Long selfId = Shadbot.getClients().get(0).getSelfId().map(Snowflake::asLong).orElse(0L);
					final JSONObject content = new JSONObject()
							.put("server_count", guildsCount);
					final String url = String.format("https://botlist.space/bots/%d/stats", selfId);

					try {
						this.post(url, Shadbot.getAPIKeys().get(APIKey.BOT_LIST_DOT_SPACE), content);
					} catch (IOException err) {
						Exceptions.propagate(err);
					}
				})
				.doOnError(err -> LogUtils.error(err, "An error occurred while posting statistics on botlist.space"))
				.then();
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
		task.dispose();
	}

}
