package me.shadorc.shadbot;

import me.shadorc.shadbot.core.shard.Shard;
import me.shadorc.shadbot.data.credential.Credential;
import me.shadorc.shadbot.data.credential.Credentials;
import me.shadorc.shadbot.utils.ExceptionHandler;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;

public class BotListStats {

    private final long selfId;
    private final Disposable task;

    public BotListStats() {
        this.selfId = Shadbot.getClient().getSelfId().get().asLong();
        this.task = Flux.interval(Duration.ofHours(3), Duration.ofHours(3), Schedulers.elastic())
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
                        .then(this.postOnDiscordBotListDotCom(guildCount))
                        .then(this.postOnDiscordBotsDotGg(guildCount))
                        .then(this.postOnDiscordBotsDotOrg(guildCount))
                        .then(this.postOnDivineDiscordBotsDotCom(guildCount)))
                .then(Mono.fromRunnable(() -> LogUtils.info("Statistics posted.")));
    }

    // TODO: Use reactor.netty
    private static Mono<Document> post(String url, String authorization, JSONObject content) {
        return Mono.fromCallable(() -> Jsoup.connect(url)
                .ignoreContentType(true)
                .requestBody(content.toString())
                .headers(Map.of("Content-Type", "application/json", "Authorization", authorization))
                .post())
                .onErrorResume(err -> Mono.fromRunnable(() -> LogUtils.warn(Shadbot.getClient(),
                        String.format("An error occurred while posting statistics on %s: %s", url, err.getMessage()))));
    }

    /**
     * WebSite: https://botlist.space/bots <br>
     * Documentation: https://docs.botlist.space/bl-docs/bots
     */
    private Mono<Document> postOnBotListDotSpace(Long guildCount) {
        final JSONObject content = new JSONObject()
                .put("server_count", guildCount);
        final String url = String.format("https://api.botlist.space/v1/bots/%d", this.selfId);
        return BotListStats.post(url, Credentials.get(Credential.BOT_LIST_DOT_SPACE), content);
    }

    /**
     * WebSite: https://bots.ondiscord.xyz/ <br>
     * Documentation: https://bots.ondiscord.xyz/info/api
     */
    private Mono<Document> postOnBotsOnDiscordXyz(Long guildCount) {
        final JSONObject content = new JSONObject()
                .put("guildCount", guildCount);
        final String url = String.format("https://bots.ondiscord.xyz/bot-api/bots/%d/guilds", this.selfId);
        return BotListStats.post(url, Credentials.get(Credential.BOTS_ONDISCORD_DOT_XYZ), content);
    }

    /**
     * WebSite: https://divinediscordbots.com/ <br>
     * Documentation: https://divinediscordbots.com/api
     */
    private Mono<Document> postOnDivineDiscordBotsDotCom(Long guildCount) {
        final JSONObject content = new JSONObject()
                .put("server_count", guildCount);
        final String url = String.format("https://divinediscordbots.com/bot/%d/stats", this.selfId);
        return BotListStats.post(url, Credentials.get(Credential.DIVINE_DISCORD_BOTS_DOT_COM_TOKEN), content);
    }

    /**
     * WebSite: https://discordbotlist.com/ <br>
     * Documentation: https://discordbotlist.com/api-docs
     */
    private Mono<Void> postOnDiscordBotListDotCom(Long guildCount) {
        return Flux.fromIterable(Shadbot.getShards().values())
                .map(Shard::getClient)
                .flatMap(client -> {
                    final JSONObject content = new JSONObject()
                            .put("shard_id", client.getConfig().getShardIndex())
                            .put("guilds ", guildCount);
                    final String url = String.format("https://discordbotlist.com/api/bots/%d/stats", this.selfId);
                    return BotListStats.post(url, String.format("Bot %s", Credentials.get(Credential.DISCORD_BOT_LIST_DOT_COM_TOKEN)), content);
                })
                .then();
    }

    /**
     * WebSite: https://discord.bots.gg/ <br>
     * Documentation: https://discord.bots.gg/docs/endpoints
     */
    private Mono<Void> postOnDiscordBotsDotGg(Long guildCount) {
        return Flux.fromIterable(Shadbot.getShards().values())
                .map(Shard::getClient)
                .flatMap(client -> {
                    final JSONObject content = new JSONObject()
                            .put("shardId", client.getConfig().getShardIndex())
                            .put("shardCount", client.getConfig().getShardCount())
                            .put("guildCount", (int) (guildCount / client.getConfig().getShardCount()));
                    final String url = String.format("https://discord.bots.gg/api/v1/bots/%d/stats", this.selfId);
                    return BotListStats.post(url, Credentials.get(Credential.DISCORD_BOTS_DOT_GG_TOKEN), content);
                })
                .then();
    }

    /**
     * WebSite: https://discordbots.org/ <br>
     * Documentation: https://discordbots.org/api/docs#bots
     */
    private Mono<Void> postOnDiscordBotsDotOrg(Long guildCount) {
        return Flux.fromIterable(Shadbot.getShards().values())
                .map(Shard::getClient)
                .flatMap(client -> {
                    final JSONObject content = new JSONObject()
                            .put("shard_id", client.getConfig().getShardIndex())
                            .put("shard_count", client.getConfig().getShardCount())
                            .put("server_count", (int) (guildCount / client.getConfig().getShardCount()));
                    final String url = String.format("https://discordbots.org/api/bots/%d/stats", this.selfId);
                    return BotListStats.post(url, Credentials.get(Credential.DISCORD_BOTS_DOT_ORG_TOKEN), content);
                })
                .then();
    }

    public void stop() {
        this.task.dispose();
    }

}
