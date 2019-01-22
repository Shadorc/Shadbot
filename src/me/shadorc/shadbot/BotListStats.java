package me.shadorc.shadbot;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;

import discord4j.core.DiscordClient;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.data.credential.Credential;
import me.shadorc.shadbot.data.credential.Credentials;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.exception.ExceptionHandler;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class BotListStats {

	private final List<DiscordClient> clients;
	private final Snowflake selfId;
	private final Disposable task;

	public BotListStats(List<DiscordClient> clients) {
		this.clients = clients;
		this.selfId = clients.get(0).getSelfId().get();
		this.task = Flux.interval(Duration.ofHours(2), Duration.ofHours(2))
				.flatMap(ignored -> this.postStats())
				.onErrorContinue((err, obj) -> ExceptionHandler.handleUnknownError(Shadbot.getClient(), err))
				.subscribe(null, err -> ExceptionHandler.handleUnknownError(Shadbot.getClient(), err));
	}

	private Mono<Void> postStats() {
		if(Config.IS_SNAPSHOT) {
			return Mono.empty();
		}

		return this.clients.get(0).getGuilds()
				.count()
				.doOnNext(guildCount -> {
					LogUtils.info("Posting statistics...");
					this.postOnBotListDotSpace(guildCount);
					this.postOnBotsOndiscordXyz(guildCount);
					this.postOnDiscordBotListDotCom(guildCount);
					this.postOnDiscordBotsDotGg(guildCount);
					this.postOnDiscordBotsDotOrg(guildCount);
					this.postOnDivineDiscordBotsDotCom(guildCount);
					LogUtils.info("Statistics posted.");
				})
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

	/**
	 * WebSite: https://botlist.space/ <br>
	 * Documentation: https://botlist.space/documentation
	 */
	private void postOnBotListDotSpace(Long guildCount) {
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
	 * @param guildCount
	 */
	private void postOnBotsOndiscordXyz(Long guildCount) {
		final JSONObject content = new JSONObject()
				.put("guildCount", guildCount);
		final String url = String.format("https://bots.ondiscord.xyz/bot-api/bots/%d/guilds", this.selfId);

		try {
			this.post(url, Credentials.get(Credential.BOTS_ONDISCORD_DOT_XYZ), content);
		} catch (final IOException err) {
			LogUtils.error(err, "An error occurred while posting statistics on bots.ondiscord.xyz");
		}
	}

	/**
	 * WebSite: https://discordbotlist.com/ <br>
	 * Documentation: https://discordbotlist.com/api-docs
	 */
	private void postOnDiscordBotListDotCom(Long guildCount) {
		for(final DiscordClient client : this.clients) {
			final JSONObject content = new JSONObject()
					.put("shard_id", client.getConfig().getShardIndex())
					.put("guilds ", guildCount);
			final String url = String.format("https://discordbotlist.com/api/bots/%d/stats", this.selfId);

			try {
				this.post(url, String.format("Bot %s", Credentials.get(Credential.DISCORD_BOT_LIST_DOT_COM_TOKEN)), content);
			} catch (final IOException err) {
				LogUtils.error(err, "An error occurred while posting statistics on discordbotlist.com");
			}
		}
	}

	/**
	 * WebSite: https://discord.bots.gg/ <br>
	 * Documentation: https://discord.bots.gg/docs/endpoints
	 */
	private void postOnDiscordBotsDotGg(Long guildCount) {
		for(final DiscordClient client : this.clients) {
			final JSONObject content = new JSONObject()
					.put("shardId", client.getConfig().getShardIndex())
					.put("shardCount", client.getConfig().getShardCount())
					.put("guildCount", (int) (guildCount / client.getConfig().getShardCount()));
			final String url = String.format("https://discord.bots.gg/api/bots/%d/stats", this.selfId);

			try {
				this.post(url, Credentials.get(Credential.DISCORD_BOTS_DOT_GG_TOKEN), content);
			} catch (final IOException err) {
				LogUtils.error(err, "An error occurred while posting statistics on discord.bots.gg");
			}
		}
	}

	/**
	 * WebSite: https://discordbots.org/ <br>
	 * Documentation: https://discordbots.org/api/docs#bots
	 */
	private void postOnDiscordBotsDotOrg(Long guildCount) {
		for(final DiscordClient client : this.clients) {
			final JSONObject content = new JSONObject()
					.put("shard_id", client.getConfig().getShardIndex())
					.put("shard_count", client.getConfig().getShardCount())
					.put("server_count", (int) (guildCount / client.getConfig().getShardCount()));
			final String url = String.format("https://discordbots.org/api/bots/%d/stats", this.selfId);

			try {
				this.post(url, Credentials.get(Credential.DISCORD_BOTS_DOT_ORG_TOKEN), content);
			} catch (final IOException err) {
				LogUtils.error(err, "An error occurred while posting statistics on discordbots.org");
			}
		}
	}

	/**
	 * WebSite: https://divinediscordbots.com/ <br>
	 * Documentation: https://divinediscordbots.com/api
	 */
	private void postOnDivineDiscordBotsDotCom(Long guildCount) {
		final JSONObject content = new JSONObject()
				.put("server_count", guildCount);
		final String url = String.format("https://divinediscordbots.com/bots/%d/stats", this.selfId);

		try {
			this.post(url, Credentials.get(Credential.DIVINE_DISCORD_BOTS_DOT_COM_TOKEN), content);
		} catch (final IOException err) {
			LogUtils.error(err, "An error occurred while posting statistics on divinediscordbots.com");
		}
	}

	public void stop() {
		this.task.dispose();
	}

}
