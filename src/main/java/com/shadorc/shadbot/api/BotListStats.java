package com.shadorc.shadbot.api;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import discord4j.core.GatewayDiscordClient;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import org.json.JSONObject;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class BotListStats {

    private final GatewayDiscordClient gateway;
    private final HttpClient httpClient;

    public BotListStats(GatewayDiscordClient gateway) {
        this.gateway = gateway;
        this.httpClient = HttpClient.create();
    }

    public Mono<Void> postStats() {
        DEFAULT_LOGGER.info("Posting statistics...");
        return this.gateway.getGuilds()
                .count()
                .flatMap(guildCount -> this.postOnBotListDotSpace(guildCount)
                        .and(this.postOnBotsOnDiscordXyz(guildCount))
                        .and(this.postOnDiscordBotListDotCom(guildCount))
                        .and(this.postOnDiscordBotsDotGg(guildCount))
                        .and(this.postOnDiscordBotsDotOrg(guildCount)))
                .doOnSuccess(ignored -> DEFAULT_LOGGER.info("Statistics posted"));
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
                        return Mono.fromRunnable(() ->
                                DEFAULT_LOGGER.warn("A timeout occurred while posting statistics on {}", url));

                    }
                    return Mono.fromRunnable(() ->
                            DEFAULT_LOGGER.warn("An error occurred while posting statistics on {}: {}", url, err.getMessage()));
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
        return this.post(url, CredentialManager.getInstance().get(Credential.BOT_LIST_DOT_SPACE), content);
    }

    /**
     * WebSite: https://bots.ondiscord.xyz/ <br>
     * Documentation: https://bots.ondiscord.xyz/info/api
     */
    private Mono<String> postOnBotsOnDiscordXyz(Long guildCount) {
        final JSONObject content = new JSONObject()
                .put("guildCount", guildCount);
        final String url = String.format("https://bots.ondiscord.xyz/bot-api/bots/%d/guilds", Shadbot.getSelfId().asLong());
        return this.post(url, CredentialManager.getInstance().get(Credential.BOTS_ONDISCORD_DOT_XYZ), content);
    }

    /**
     * WebSite: https://discordbotlist.com/ <br>
     * Documentation: https://discordbotlist.com/api-docs
     */
    private Flux<String> postOnDiscordBotListDotCom(Long guildCount) {
        final int shardCount = this.gateway.getGatewayClientGroup().getShardCount();
        return Flux.fromStream(IntStream.range(0, shardCount).boxed())
                .flatMap(shardId -> {
                    final JSONObject content = new JSONObject()
                            .put("shard_id", shardId)
                            .put("guilds ", guildCount / shardCount);
                    final String url = String.format("https://discordbotlist.com/api/bots/%d/stats",
                            Shadbot.getSelfId().asLong());
                    return this.post(url, String.format("Bot %s",
                            CredentialManager.getInstance().get(Credential.DISCORD_BOT_LIST_DOT_COM_TOKEN)), content);
                });
    }

    /**
     * WebSite: https://discord.bots.gg/ <br>
     * Documentation: https://discord.bots.gg/docs/endpoints
     */
    private Flux<String> postOnDiscordBotsDotGg(Long guildCount) {
        final int shardCount = this.gateway.getGatewayClientGroup().getShardCount();
        return Flux.fromStream(IntStream.range(0, shardCount).boxed())
                .flatMap(shardId -> {
                    final JSONObject content = new JSONObject()
                            .put("shardId", shardId)
                            .put("shardCount", shardCount)
                            .put("guildCount", guildCount / shardCount);
                    final String url = String.format("https://discord.bots.gg/api/v1/bots/%d/stats", Shadbot.getSelfId().asLong());
                    return this.post(url, CredentialManager.getInstance().get(Credential.DISCORD_BOTS_DOT_GG_TOKEN), content);
                });
    }

    /**
     * WebSite: https://top.gg/ <br>
     * Documentation: https://top.gg/api/docs#bots
     */
    private Flux<String> postOnDiscordBotsDotOrg(Long guildCount) {
        final int shardCount = this.gateway.getGatewayClientGroup().getShardCount();
        return Flux.fromStream(IntStream.range(0, shardCount).boxed())
                .flatMap(shardId -> {
                    final JSONObject content = new JSONObject()
                            .put("shard_id", shardId)
                            .put("shard_count", shardCount)
                            .put("server_count", guildCount / shardCount);
                    final String url = String.format("https://top.gg/api/bots/%d/stats", Shadbot.getSelfId().asLong());
                    return this.post(url, CredentialManager.getInstance().get(Credential.TOP_DOT_GG_TOKEN), content);
                });
    }

}
