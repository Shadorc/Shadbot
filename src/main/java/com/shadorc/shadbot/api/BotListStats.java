package com.shadorc.shadbot.api;

import com.shadorc.shadbot.api.json.dbl.TopGgWebhookResponse;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.users.entity.achievement.Achievement;
import com.shadorc.shadbot.object.ExceptionHandler;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.LogUtil;
import com.shadorc.shadbot.utils.NetUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.json.JSONObject;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.util.Logger;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.IntStream;

public class BotListStats {

    private static final Logger LOGGER = LogUtil.getLogger(BotListStats.class);

    private static final String BOTLIST_DOT_SPACE = "https://botlist.space";
    private static final String BOTS_ONDISCORD_DOT_XYZ = "https://bots.ondiscord.xyz";
    private static final String WONDERBOTLIST_DOT_COM = "https://wonderbotlist.com";
    private static final String DISCORDBOTLIST_DOT_COM = "https://discordbotlist.com";
    private static final String DISCORD_BOTS_DOT_GG = "https://discord.bots.gg";
    private static final String TOP_DOT_GG = "https://top.gg";
    private static final String BOTSFORDISCORD_DOT_COM = "https://botsfordiscord.com";
    private static final String DISCORD_DOT_BOATS = "https://discord.boats";
    private static final String DISCORDEXTREMELIST_DOT_XYZ = "https://discordextremelist.xyz";

    private final GatewayDiscordClient gateway;
    private final AtomicReference<DisposableServer> webhookServer;

    public BotListStats(GatewayDiscordClient gateway) {
        this.gateway = gateway;
        this.webhookServer = new AtomicReference<>();

        this.setupTopGgWebhook();
    }

    private void setupTopGgWebhook() {
        final String authorization = CredentialManager.getInstance().get(Credential.TOP_DOT_GG_WEBHOOK_AUTHORIZATION);
        final String port = CredentialManager.getInstance().get(Credential.TOP_DOT_GG_WEBHOOK_PORT);
        if (authorization != null && port != null) {
            LOGGER.info("Initializing top.gg WebHook server");
            HttpServer.create()
                    .port(Integer.parseInt(port))
                    .route(routes -> routes.post("/webhook",
                            (request, response) -> {
                                if (authorization.equals(request.requestHeaders().get(HttpHeaderNames.AUTHORIZATION))) {
                                    return request.receive()
                                            .asString()
                                            .doOnNext(content -> LOGGER.debug("Webhook event received: {}", content))
                                            .flatMap(content -> Mono.fromCallable(() ->
                                                    NetUtil.MAPPER.readValue(content, TopGgWebhookResponse.class)))
                                            .map(TopGgWebhookResponse::getUserId)
                                            .map(Snowflake::of)
                                            .flatMap(DatabaseManager.getUsers()::getDBUser)
                                            .flatMap(dbUser -> dbUser.unlockAchievement(Achievement.VOTER))
                                            .then(response.status(HttpResponseStatus.OK).send());
                                }
                                return response.status(HttpResponseStatus.FORBIDDEN).send();
                            }))
                    .bind()
                    .doOnNext(this.webhookServer::set)
                    .subscribe(null, ExceptionHandler::handleUnknownError);
        }
    }

    public Mono<Void> postStats() {
        LOGGER.info("Posting statistics");
        final int shardCount = this.gateway.getGatewayClientGroup().getShardCount();
        return this.gateway.getGuilds().count()
                .flatMap(guildCount -> this.postOnBotlistDotSpace(guildCount)
                        .and(this.postOnBotsOndiscordDotXyz(guildCount))
                        .and(this.postOnDiscordbotlistDotCom(shardCount, guildCount))
                        .and(this.postOnWonderbotlistDotCom(shardCount, guildCount))
                        .and(this.postOnBotsfordiscordDotCom(guildCount))
                        .and(this.postOnDiscordDotBoats(guildCount))
                        .and(this.postOnDiscordextremelistDotXyz(guildCount))
                        .and(this.postOnDiscordBotsDotGg(shardCount, guildCount))
                        .and(this.postOnTopDotGg(shardCount, guildCount)))
                .doOnSuccess(__ -> LOGGER.info("Statistics posted"));
    }

    /**
     * Documentation: https://docs.botlist.space/bl-docs/bots
     */
    private Mono<String> postOnBotlistDotSpace(long guildCount) {
        final JSONObject content = new JSONObject()
                .put("server_count", guildCount);
        final String url = String.format("https://api.botlist.space/v1/bots/%d", this.gateway.getSelfId().asLong());
        return BotListStats.post(url, CredentialManager.getInstance().get(Credential.BOTLIST_DOT_SPACE_TOKEN), content)
                .onErrorResume(BotListStats.handleError(BOTLIST_DOT_SPACE));
    }

    /**
     * Documentation: https://bots.ondiscord.xyz/info/api
     */
    private Mono<String> postOnBotsOndiscordDotXyz(long guildCount) {
        final JSONObject content = new JSONObject()
                .put("guildCount", guildCount);
        final String url = String.format("https://bots.ondiscord.xyz/bot-api/bots/%d/guilds", this.gateway.getSelfId().asLong());
        return BotListStats.post(url, CredentialManager.getInstance().get(Credential.BOTS_ONDISCORD_DOT_XYZ_TOKEN), content)
                .onErrorResume(BotListStats.handleError(BOTS_ONDISCORD_DOT_XYZ));
    }

    /**
     * Documentation: https://api.wonderbotlist.com/fr/#bots
     */
    private Mono<String> postOnWonderbotlistDotCom(int shardCount, long guildCount) {
        final JSONObject content = new JSONObject()
                .put("serveurs", guildCount)
                .put("shards", shardCount);
        final String url = String.format("https://api.wonderbotlist.com/v1/bot/%d", this.gateway.getSelfId().asLong());
        return BotListStats.post(url, CredentialManager.getInstance().get(Credential.WONDERBOTLIST_DOT_COM_TOKEN), content)
                .onErrorResume(BotListStats.handleError(WONDERBOTLIST_DOT_COM));
    }

    /**
     * Documentation: https://docs.discord.boats/endpoints/bots
     */
    private Mono<String> postOnDiscordDotBoats(long guildCount) {
        final JSONObject content = new JSONObject()
                .put("server_count", guildCount);
        final String url = String.format("https://discord.boats/api/bot/%d", this.gateway.getSelfId().asLong());
        return BotListStats.post(url, CredentialManager.getInstance().get(Credential.DISCORD_DOT_BOATS), content)
                .onErrorResume(BotListStats.handleError(DISCORD_DOT_BOATS));
    }

    /**
     * Documentation: https://discordextremelist.xyz/en-US/docs#api-routes
     */
    private Mono<String> postOnDiscordextremelistDotXyz(long guildCount) {
        final JSONObject content = new JSONObject()
                .put("guildCount", guildCount);
        final String url = String.format("https://api.discordextremelist.xyz/v2/bot/%d/stats", this.gateway.getSelfId().asLong());
        return BotListStats.post(url, CredentialManager.getInstance().get(Credential.DISCORDEXTREMELIST_DOT_XYZ), content)
                .onErrorResume(BotListStats.handleError(DISCORDEXTREMELIST_DOT_XYZ));
    }

    /**
     * Documentation: https://docs.botsfordiscord.com/methods/bots
     */
    private Mono<String> postOnBotsfordiscordDotCom(long guildCount) {
        final JSONObject content = new JSONObject()
                .put("server_count", guildCount);
        final String url = String.format("https://botsfordiscord.com/api/bot/%d", this.gateway.getSelfId().asLong());
        return BotListStats.post(url, CredentialManager.getInstance().get(Credential.BOTSFORDISCORD_DOT_COM), content)
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
                    final String url = String.format("https://discordbotlist.com/api/bots/%d/stats",
                            this.gateway.getSelfId().asLong());
                    return BotListStats.post(url, String.format("Bot %s",
                            CredentialManager.getInstance().get(Credential.DISCORDBOTLIST_DOT_COM_TOKEN)), content);
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
                    final String url = String.format("https://discord.bots.gg/api/v1/bots/%d/stats", this.gateway.getSelfId().asLong());
                    return BotListStats.post(url, CredentialManager.getInstance().get(Credential.DISCORD_BOTS_DOT_GG_TOKEN), content);
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
                    final String url = String.format("https://top.gg/api/bots/%d/stats", this.gateway.getSelfId().asLong());
                    return BotListStats.post(url, CredentialManager.getInstance().get(Credential.TOP_DOT_GG_TOKEN), content);
                })
                .onErrorResume(BotListStats.handleError(TOP_DOT_GG));
    }

    public void stop() {
        if (this.webhookServer.get() != null) {
            this.webhookServer.get().disposeNow();
        }
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
