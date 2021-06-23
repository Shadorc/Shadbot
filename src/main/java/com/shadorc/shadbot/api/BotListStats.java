package com.shadorc.shadbot.api;

import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.LogUtil;
import discord4j.core.GatewayDiscordClient;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import org.json.JSONObject;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.IntStream;

public class BotListStats {

    private static final Logger LOGGER = LogUtil.getLogger(BotListStats.class);

    private static final String DISCORDLIST_DOT_SPACE = "https://discordlist.space";
    private static final String BOTS_ONDISCORD_DOT_XYZ = "https://bots.ondiscord.xyz";
    private static final String WONDERBOTLIST_DOT_COM = "https://wonderbotlist.com";
    private static final String DISCORDBOTLIST_DOT_COM = "https://discordbotlist.com";
    private static final String DISCORD_BOTS_DOT_GG = "https://discord.bots.gg";
    private static final String TOP_DOT_GG = "https://top.gg";
    private static final String BOTSFORDISCORD_DOT_COM = "https://botsfordiscord.com";
    private static final String DISCORD_DOT_BOATS = "https://discord.boats";
    private static final String DISCORDEXTREMELIST_DOT_XYZ = "https://discordextremelist.xyz";

    private final GatewayDiscordClient gateway;
    private final long selfId;

    public BotListStats(GatewayDiscordClient gateway) {
        this.gateway = gateway;
        this.selfId = gateway.getSelfId().asLong();
    }

    public Mono<Void> postStats() {
        LOGGER.info("Posting statistics");
        final int shardCount = this.gateway.getGatewayClientGroup().getShardCount();
        return this.gateway.getGuilds().count()
                .flatMap(guildCount -> Mono.when(
                        this.postOnDiscordlistDotSpace(guildCount),
                        this.postOnBotsOndiscordDotXyz(guildCount),
                        this.postOnDiscordbotlistDotCom(shardCount, guildCount),
                        this.postOnWonderbotlistDotCom(shardCount, guildCount),
                        this.postOnBotsfordiscordDotCom(guildCount),
                        this.postOnDiscordDotBoats(guildCount),
                        this.postOnDiscordextremelistDotXyz(guildCount),
                        this.postOnDiscordBotsDotGg(shardCount, guildCount),
                        this.postOnTopDotGg(shardCount, guildCount)))
                .doOnSuccess(__ -> LOGGER.info("Statistics posted"));
    }

    /**
     * Documentation: https://docs.discordlist.space/bl-docs/bots
     */
    private Mono<String> postOnDiscordlistDotSpace(long guildCount) {
        final JSONObject content = new JSONObject()
                .put("server_count", guildCount);
        final String url = "https://api.discordlist.space/v1/bots/%d".formatted(this.selfId);
        return BotListStats.post(url, CredentialManager.get(Credential.DISCORDLIST_DOT_SPACE_TOKEN), content)
                .onErrorResume(BotListStats.handleError(DISCORDLIST_DOT_SPACE));
    }

    /**
     * Documentation: https://bots.ondiscord.xyz/info/api
     */
    private Mono<String> postOnBotsOndiscordDotXyz(long guildCount) {
        final JSONObject content = new JSONObject()
                .put("guildCount", guildCount);
        final String url = "https://bots.ondiscord.xyz/bot-api/bots/%d/guilds".formatted(this.selfId);
        return BotListStats.post(url, CredentialManager.get(Credential.BOTS_ONDISCORD_DOT_XYZ_TOKEN), content)
                .onErrorResume(BotListStats.handleError(BOTS_ONDISCORD_DOT_XYZ));
    }

    /**
     * Documentation: https://api.wonderbotlist.com/fr/#bots
     */
    private Mono<String> postOnWonderbotlistDotCom(int shardCount, long guildCount) {
        final JSONObject content = new JSONObject()
                .put("serveurs", guildCount)
                .put("shards", shardCount);
        final String url = "https://api.wonderbotlist.com/v1/bot/%d".formatted(this.selfId);
        return BotListStats.post(url, CredentialManager.get(Credential.WONDERBOTLIST_DOT_COM_TOKEN), content)
                .onErrorResume(BotListStats.handleError(WONDERBOTLIST_DOT_COM));
    }

    /**
     * Documentation: https://docs.discord.boats/endpoints/bots
     */
    private Mono<String> postOnDiscordDotBoats(long guildCount) {
        final JSONObject content = new JSONObject()
                .put("server_count", guildCount);
        final String url = "https://discord.boats/api/bot/%d".formatted(this.selfId);
        return BotListStats.post(url, CredentialManager.get(Credential.DISCORD_DOT_BOATS), content)
                .onErrorResume(BotListStats.handleError(DISCORD_DOT_BOATS));
    }

    /**
     * Documentation: https://discordextremelist.xyz/en-US/docs#api-routes
     */
    private Mono<String> postOnDiscordextremelistDotXyz(long guildCount) {
        final JSONObject content = new JSONObject()
                .put("guildCount", guildCount);
        final String url = "https://api.discordextremelist.xyz/v2/bot/%d/stats".formatted(this.selfId);
        return BotListStats.post(url, CredentialManager.get(Credential.DISCORDEXTREMELIST_DOT_XYZ), content)
                .onErrorResume(BotListStats.handleError(DISCORDEXTREMELIST_DOT_XYZ));
    }

    /**
     * Documentation: https://docs.botsfordiscord.com/methods/bots
     */
    private Mono<String> postOnBotsfordiscordDotCom(long guildCount) {
        final JSONObject content = new JSONObject()
                .put("server_count", guildCount);
        final String url = "https://botsfordiscord.com/api/bot/%d".formatted(this.selfId);
        return BotListStats.post(url, CredentialManager.get(Credential.BOTSFORDISCORD_DOT_COM), content)
                .onErrorResume(BotListStats.handleError(BOTSFORDISCORD_DOT_COM));
    }

    /**
     * Documentation: https://discordbotlist.com/api-docs
     */
    private Flux<String> postOnDiscordbotlistDotCom(int shardCount, long guildCount) {
        return Flux.fromStream(IntStream.range(0, shardCount).boxed())
                .flatMap(shardId -> {
                    final JSONObject content = new JSONObject()
                            .put("shard_id", shardId)
                            .put("guilds ", guildCount / shardCount);
                    final String url = "https://discordbotlist.com/api/bots/%d/stats"
                            .formatted(this.selfId);
                    return BotListStats.post(url, "Bot %s"
                            .formatted(CredentialManager.get(Credential.DISCORDBOTLIST_DOT_COM_TOKEN)), content);
                })
                .onErrorResume(BotListStats.handleError(DISCORDBOTLIST_DOT_COM));
    }

    /**
     * Documentation: https://discord.bots.gg/docs/endpoints
     */
    private Flux<String> postOnDiscordBotsDotGg(int shardCount, long guildCount) {
        return Flux.fromStream(IntStream.range(0, shardCount).boxed())
                .flatMap(shardId -> {
                    final JSONObject content = new JSONObject()
                            .put("shardId", shardId)
                            .put("shardCount", shardCount)
                            .put("guildCount", guildCount / shardCount);
                    final String url = "https://discord.bots.gg/api/v1/bots/%d/stats".formatted(this.selfId);
                    return BotListStats.post(url, CredentialManager.get(Credential.DISCORD_BOTS_DOT_GG_TOKEN), content);
                })
                .onErrorResume(BotListStats.handleError(DISCORD_BOTS_DOT_GG));
    }

    /**
     * Documentation: https://top.gg/api/docs#bots
     */
    private Flux<String> postOnTopDotGg(int shardCount, long guildCount) {
        return Flux.fromStream(IntStream.range(0, shardCount).boxed())
                .flatMap(shardId -> {
                    final JSONObject content = new JSONObject()
                            .put("shard_id", shardId)
                            .put("shard_count", shardCount)
                            .put("server_count", guildCount / shardCount);
                    final String url = "https://top.gg/api/bots/%d/stats".formatted(this.selfId);
                    return BotListStats.post(url, CredentialManager.get(Credential.TOP_DOT_GG_TOKEN), content);
                })
                .onErrorResume(BotListStats.handleError(TOP_DOT_GG));
    }

    private static Mono<String> post(String url, String authorization, JSONObject content) {
        return RequestHelper.fromUrl(url)
                .setMethod(HttpMethod.POST)
                .addHeaders(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                .addHeaders(HttpHeaderNames.AUTHORIZATION, authorization)
                .request()
                .send((req, res) -> res.sendString(Mono.just(content.toString())))
                .responseSingle((res, con) -> con.asString())
                .timeout(Config.TIMEOUT)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(5))
                        .filter(TimeoutException.class::isInstance));
    }

    private static <T> Function<? super Throwable, ? extends Mono<? extends T>> handleError(String url) {
        return err -> {
            if (err instanceof TimeoutException) {
                return Mono.fromRunnable(() ->
                        LOGGER.warn("A timeout occurred while posting statistics on {}", url));
            }
            return Mono.fromRunnable(() ->
                    LOGGER.warn("An error occurred while posting statistics on {}: {}", url, err.getMessage()));
        };
    }

}
