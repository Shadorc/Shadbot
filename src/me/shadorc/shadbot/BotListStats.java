package me.shadorc.shadbot;

import discord4j.core.DiscordClient;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import me.shadorc.shadbot.core.shard.Shard;
import me.shadorc.shadbot.data.credential.Credential;
import me.shadorc.shadbot.data.credential.Credentials;
import me.shadorc.shadbot.utils.ExceptionHandler;
import me.shadorc.shadbot.utils.LogUtils;
import org.json.JSONObject;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

public class BotListStats {

    private final DiscordClient client;
    private final HttpClient httpClient;
    private final long selfId;
    private final Disposable task;

    public BotListStats(DiscordClient client) {
        this.client = client;
        this.httpClient = HttpClient.create();
        this.selfId = this.client.getSelfId().get().asLong();
        this.task = Flux.interval(Duration.ofHours(3), Duration.ofHours(3), Schedulers.elastic())
                .flatMap(ignored -> this.postStats())
                .subscribe(null, err -> ExceptionHandler.handleUnknownError(this.client, err));
    }

    private Mono<Void> postStats() {
        LogUtils.info("Posting statistics...");
        return Shadbot.getClient()
                .getGuilds()
                .count()
                .flatMap(guildCount -> this.postOnBotListDotSpace(guildCount)
                        .then(this.postOnBotsOnDiscordXyz(guildCount))
                        .then(this.postOnDivineDiscordBotsDotCom(guildCount))
                        .thenMany(this.postOnDiscordBotListDotCom(guildCount))
                        .thenMany(this.postOnDiscordBotsDotGg(guildCount))
                        .thenMany(this.postOnDiscordBotsDotOrg(guildCount))
                        .then())
                .then(Mono.fromRunnable(() -> LogUtils.info("Statistics posted.")));
    }

    private Mono<String> post(String url, String authorization, JSONObject content) {
        return this.httpClient
                .headers(header -> header.add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                        .add(HttpHeaderNames.AUTHORIZATION, authorization))
                .post()
                .uri(url)
                .send((req, res) -> res.sendString(Mono.just(content.toString()), StandardCharsets.UTF_8))
                .responseSingle((res, con) -> con.asString(StandardCharsets.UTF_8))
                .timeout(Config.DEFAULT_TIMEOUT)
                .onErrorResume(err -> {
                    if (err instanceof TimeoutException) {
                        return Mono.fromRunnable(() -> LogUtils.warn(this.client,
                                String.format("A timeout occurred while posting statistics on %s", url)));

                    }
                    return Mono.fromRunnable(() -> LogUtils.warn(this.client,
                            String.format("An error occurred while posting statistics on %s: %s", url, err.getMessage())));
                });
    }

    /**
     * WebSite: https://botlist.space/bots <br>
     * Documentation: https://docs.botlist.space/bl-docs/bots
     */
    private Mono<String> postOnBotListDotSpace(Long guildCount) {
        final JSONObject content = new JSONObject()
                .put("server_count", guildCount);
        final String url = String.format("https://api.botlist.space/v1/bots/%d", this.selfId);
        return this.post(url, Credentials.get(Credential.BOT_LIST_DOT_SPACE), content);
    }

    /**
     * WebSite: https://bots.ondiscord.xyz/ <br>
     * Documentation: https://bots.ondiscord.xyz/info/api
     */
    private Mono<String> postOnBotsOnDiscordXyz(Long guildCount) {
        final JSONObject content = new JSONObject()
                .put("guildCount", guildCount);
        final String url = String.format("https://bots.ondiscord.xyz/bot-api/bots/%d/guilds", this.selfId);
        return this.post(url, Credentials.get(Credential.BOTS_ONDISCORD_DOT_XYZ), content);
    }

    /**
     * WebSite: https://divinediscordbots.com/ <br>
     * Documentation: https://divinediscordbots.com/api
     */
    private Mono<String> postOnDivineDiscordBotsDotCom(Long guildCount) {
        final JSONObject content = new JSONObject()
                .put("server_count", guildCount);
        final String url = String.format("https://divinediscordbots.com/bot/%d/stats", this.selfId);
        return this.post(url, Credentials.get(Credential.DIVINE_DISCORD_BOTS_DOT_COM_TOKEN), content);
    }

    /**
     * WebSite: https://discordbotlist.com/ <br>
     * Documentation: https://discordbotlist.com/api-docs
     */
    private Flux<String> postOnDiscordBotListDotCom(Long guildCount) {
        return Flux.fromIterable(Shadbot.getShards().values())
                .map(Shard::getClient)
                .flatMap(client -> {
                    final JSONObject content = new JSONObject()
                            .put("shard_id", client.getConfig().getShardIndex())
                            .put("guilds ", guildCount);
                    final String url = String.format("https://discordbotlist.com/api/bots/%d/stats", this.selfId);
                    return this.post(url, String.format("Bot %s", Credentials.get(Credential.DISCORD_BOT_LIST_DOT_COM_TOKEN)), content);
                });
    }

    /**
     * WebSite: https://discord.bots.gg/ <br>
     * Documentation: https://discord.bots.gg/docs/endpoints
     */
    private Flux<String> postOnDiscordBotsDotGg(Long guildCount) {
        return Flux.fromIterable(Shadbot.getShards().values())
                .map(Shard::getClient)
                .flatMap(client -> {
                    final JSONObject content = new JSONObject()
                            .put("shardId", client.getConfig().getShardIndex())
                            .put("shardCount", client.getConfig().getShardCount())
                            .put("guildCount", (int) (guildCount / client.getConfig().getShardCount()));
                    final String url = String.format("https://discord.bots.gg/api/v1/bots/%d/stats", this.selfId);
                    return this.post(url, Credentials.get(Credential.DISCORD_BOTS_DOT_GG_TOKEN), content);
                });
    }

    /**
     * WebSite: https://discordbots.org/ <br>
     * Documentation: https://discordbots.org/api/docs#bots
     */
    private Flux<String> postOnDiscordBotsDotOrg(Long guildCount) {
        return Flux.fromIterable(Shadbot.getShards().values())
                .map(Shard::getClient)
                .flatMap(client -> {
                    final JSONObject content = new JSONObject()
                            .put("shard_id", client.getConfig().getShardIndex())
                            .put("shard_count", client.getConfig().getShardCount())
                            .put("server_count", (int) (guildCount / client.getConfig().getShardCount()));
                    final String url = String.format("https://discordbots.org/api/bots/%d/stats", this.selfId);
                    return this.post(url, Credentials.get(Credential.DISCORD_BOTS_DOT_ORG_TOKEN), content);
                });
    }

    public void stop() {
        this.task.dispose();
    }

}
