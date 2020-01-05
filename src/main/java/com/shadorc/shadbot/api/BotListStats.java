package com.shadorc.shadbot.api;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.Credentials;
import com.shadorc.shadbot.utils.ExceptionHandler;
import com.shadorc.shadbot.utils.LogUtils;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import org.json.JSONObject;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

public class BotListStats {

    private final HttpClient httpClient;
    private final Disposable postingTask;

    public BotListStats() {
        this.httpClient = HttpClient.create();
        this.postingTask = Flux.interval(Duration.ofHours(3), Duration.ofHours(3), Schedulers.elastic())
                .flatMap(ignored -> this.postStats())
                .subscribe(null, err -> ExceptionHandler.handleUnknownError(Shadbot.getClient(), err));
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
                .timeout(Config.TIMEOUT)
                .onErrorResume(err -> {
                    if (err instanceof TimeoutException) {
                        return Mono.fromRunnable(() -> LogUtils.warn(
                                String.format("A timeout occurred while posting statistics on %s", url)));

                    }
                    return Mono.fromRunnable(() -> LogUtils.warn(Shadbot.getClient(),
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
        final String url = String.format("https://api.botlist.space/v1/bots/%d", Shadbot.getSelfId().asLong());
        return this.post(url, Credentials.get(Credential.BOT_LIST_DOT_SPACE), content);
    }

    /**
     * WebSite: https://bots.ondiscord.xyz/ <br>
     * Documentation: https://bots.ondiscord.xyz/info/api
     */
    private Mono<String> postOnBotsOnDiscordXyz(Long guildCount) {
        final JSONObject content = new JSONObject()
                .put("guildCount", guildCount);
        final String url = String.format("https://bots.ondiscord.xyz/bot-api/bots/%d/guilds", Shadbot.getSelfId().asLong());
        return this.post(url, Credentials.get(Credential.BOTS_ONDISCORD_DOT_XYZ), content);
    }

    /**
     * WebSite: https://divinediscordbots.com/ <br>
     * Documentation: https://divinediscordbots.com/api
     */
    private Mono<String> postOnDivineDiscordBotsDotCom(Long guildCount) {
        final JSONObject content = new JSONObject()
                .put("server_count", guildCount);
        final String url = String.format("https://divinediscordbots.com/bot/%d/stats", Shadbot.getSelfId().asLong());
        return this.post(url, Credentials.get(Credential.DIVINE_DISCORD_BOTS_DOT_COM_TOKEN), content);
    }

    /**
     * WebSite: https://discordbotlist.com/ <br>
     * Documentation: https://discordbotlist.com/api-docs
     */
    private Flux<String> postOnDiscordBotListDotCom(Long guildCount) {
        final int shardCount = Shadbot.getClient().getGatewayClientGroup().getShardCount();
        return Flux.fromStream(IntStream.range(0, shardCount).boxed())
                .flatMap(shardId -> {
                    final JSONObject content = new JSONObject()
                            .put("shard_id", shardId)
                            .put("guilds ", guildCount / shardCount);
                    final String url = String.format("https://discordbotlist.com/api/bots/%d/stats", Shadbot.getSelfId().asLong());
                    return this.post(url, String.format("Bot %s", Credentials.get(Credential.DISCORD_BOT_LIST_DOT_COM_TOKEN)), content);
                });
    }

    /**
     * WebSite: https://discord.bots.gg/ <br>
     * Documentation: https://discord.bots.gg/docs/endpoints
     */
    private Flux<String> postOnDiscordBotsDotGg(Long guildCount) {
        final int shardCount = Shadbot.getClient().getGatewayClientGroup().getShardCount();
        return Flux.fromStream(IntStream.range(0, shardCount).boxed())
                .flatMap(shardId -> {
                    final JSONObject content = new JSONObject()
                            .put("shardId", shardId)
                            .put("shardCount", shardCount)
                            .put("guildCount", guildCount / shardCount);
                    final String url = String.format("https://discord.bots.gg/api/v1/bots/%d/stats", Shadbot.getSelfId().asLong());
                    return this.post(url, Credentials.get(Credential.DISCORD_BOTS_DOT_GG_TOKEN), content);
                });
    }

    /**
     * WebSite: https://discordbots.org/ <br>
     * Documentation: https://discordbots.org/api/docs#bots
     */
    private Flux<String> postOnDiscordBotsDotOrg(Long guildCount) {
        final int shardCount = Shadbot.getClient().getGatewayClientGroup().getShardCount();
        return Flux.fromStream(IntStream.range(0, shardCount).boxed())
                .flatMap(shardId -> {
                    final JSONObject content = new JSONObject()
                            .put("shard_id", shardId)
                            .put("shard_count", shardCount)
                            .put("server_count", guildCount / shardCount);
                    final String url = String.format("https://discordbots.org/api/bots/%d/stats", Shadbot.getSelfId().asLong());
                    return this.post(url, Credentials.get(Credential.DISCORD_BOTS_DOT_ORG_TOKEN), content);
                });
    }

    public void stop() {
        this.postingTask.dispose();
    }

}
