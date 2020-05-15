package com.shadorc.shadbot.api;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.api.json.dbl.VoterResponse;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.NetUtils;
import discord4j.core.GatewayDiscordClient;
import discord4j.rest.util.Snowflake;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import org.json.JSONObject;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class BotListStats {

    private final GatewayDiscordClient gateway;

    public BotListStats(GatewayDiscordClient gateway) {
        this.gateway = gateway;
    }

    public Mono<Void> postStats() {
        DEFAULT_LOGGER.info("Posting statistics...");
        final int shardCount = this.gateway.getGatewayClientGroup().getShardCount();
        return DiscordUtils.getGuildCount(this.gateway)
                .flatMap(guildCount -> this.postOnBotlistDotSpace(guildCount)
                        .and(this.postOnBotsOndiscordDotXyz(guildCount))
                        .and(this.postOnDiscordbotlistDotCom(shardCount, guildCount))
                        .and(this.postOnDiscordBotsDotGg(shardCount, guildCount))
                        .and(this.postOnTopDotGg(shardCount, guildCount))
                        .and(this.postOnWonderbotlistDotCom(shardCount, guildCount)))
                .doOnSuccess(ignored -> DEFAULT_LOGGER.info("Statistics posted"));
    }

    private Mono<String> post(String url, String authorization, JSONObject content) {
        final Consumer<HttpHeaders> headersConsumer =
                header -> header.add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                        .add(HttpHeaderNames.AUTHORIZATION, authorization);
        return NetUtils.post(headersConsumer, url, content.toString())
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
    private Mono<String> postOnBotlistDotSpace(long guildCount) {
        final JSONObject content = new JSONObject()
                .put("server_count", guildCount);
        final String url = String.format("https://api.botlist.space/v1/bots/%d", Shadbot.getSelfId().asLong());
        return this.post(url, CredentialManager.getInstance().get(Credential.BOTLIST_DOT_SPACE_TOKEN), content);
    }

    /**
     * WebSite: https://bots.ondiscord.xyz/ <br>
     * Documentation: https://bots.ondiscord.xyz/info/api
     */
    private Mono<String> postOnBotsOndiscordDotXyz(long guildCount) {
        final JSONObject content = new JSONObject()
                .put("guildCount", guildCount);
        final String url = String.format("https://bots.ondiscord.xyz/bot-api/bots/%d/guilds", Shadbot.getSelfId().asLong());
        return this.post(url, CredentialManager.getInstance().get(Credential.BOTS_ONDISCORD_DOT_XYZ_TOKEN), content);
    }

    /**
     * WebSite: https://discordbotlist.com/ <br>
     * Documentation: https://discordbotlist.com/api-docs
     */
    private Flux<String> postOnDiscordbotlistDotCom(int shardCount, long guildCount) {
        return Flux.fromStream(IntStream.range(0, shardCount).boxed())
                .flatMap(shardId -> {
                    final JSONObject content = new JSONObject()
                            .put("shard_id", shardId)
                            .put("guilds ", guildCount / shardCount);
                    final String url = String.format("https://discordbotlist.com/api/bots/%d/stats",
                            Shadbot.getSelfId().asLong());
                    return this.post(url, String.format("Bot %s",
                            CredentialManager.getInstance().get(Credential.DISCORDBOTLIST_DOT_COM_TOKEN)), content);
                });
    }

    /**
     * WebSite: https://discord.bots.gg/ <br>
     * Documentation: https://discord.bots.gg/docs/endpoints
     */
    private Flux<String> postOnDiscordBotsDotGg(int shardCount, long guildCount) {
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
    private Flux<String> postOnTopDotGg(int shardCount, long guildCount) {
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

    /**
     * WebSite: https://wonderbotlist.com <br>
     * Documentation: https://api.wonderbotlist.com/fr/#bots
     */
    private Mono<String> postOnWonderbotlistDotCom(int shardCount, long guildCount) {
        final JSONObject content = new JSONObject()
                .put("serveurs", guildCount)
                .put("shards", shardCount);
        final String url = String.format("https://api.wonderbotlist.com/v1/bot/%d", Shadbot.getSelfId().asLong());
        return this.post(url, CredentialManager.getInstance().get(Credential.WONDERBOTLIST_DOT_COM_TOKEN), content);
    }

    /**
     * @return Monthly voter IDs from https://top.gg/
     */
    public Flux<Snowflake> getStats() {
        final Consumer<HttpHeaders> headersConsumer = header -> header.add(HttpHeaderNames.AUTHORIZATION,
                CredentialManager.getInstance().get(Credential.TOP_DOT_GG_TOKEN));
        return NetUtils.get(headersConsumer, String.format("https://top.gg/api/bots/%d/votes",
                Shadbot.getSelfId().asLong()), VoterResponse[].class)
                .flatMapMany(Flux::fromArray)
                .map(VoterResponse::getId)
                .map(Snowflake::of)
                .distinct();
    }

}
