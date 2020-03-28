package com.shadorc.shadbot.db.stats;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.PushOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.shadorc.shadbot.command.owner.ResourceStatsCmd;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.db.DatabaseCollection;
import com.shadorc.shadbot.db.stats.bean.command.TotalCommandStatsBean;
import com.shadorc.shadbot.db.stats.bean.resources.DailyResourceStatsBean;
import com.shadorc.shadbot.db.stats.entity.command.TotalCommandStats;
import com.shadorc.shadbot.db.stats.entity.resources.DailyResourceStats;
import com.shadorc.shadbot.utils.Utils;
import org.bson.Document;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class StatsCollection extends DatabaseCollection {

    public static final Logger LOGGER = Loggers.getLogger("shadbot.database.stats");

    public StatsCollection(MongoDatabase database) {
        super(database.getCollection("stats"));
    }

    public Mono<UpdateResult> logCommand(BaseCmd cmd) {
        LOGGER.debug("[Commands stats] Logging `{}` usage", cmd.getName());

        return Mono.from(this.getCollection()
                .updateOne(Filters.eq("_id", "command_stats"),
                        Updates.inc(String.format("command_stats.%d.%s",
                                TimeUnit.MILLISECONDS.toDays(Instant.now().toEpochMilli()), cmd.getName()), 1),
                        new UpdateOptions().upsert(true)))
                .doOnNext(result -> LOGGER.debug("[Commands stats] Logging `{}` usage result: {}", cmd.getName(), result));
    }

    public Mono<UpdateResult> logSystemResources() {
        final double cpuUsage = Utils.getCpuUsage();
        final long memoryUsed = Utils.getMemoryUsed();
        final int threadCount = Thread.activeCount();
        LOGGER.debug("[System stats] Logging CPU={}, RAM={}, Threads={}", cpuUsage, memoryUsed, threadCount);

        final int slice = (int) (ResourceStatsCmd.MAX_DURATION.toSeconds() / ResourceStatsCmd.UPDATE_INTERVAL.toSeconds());
        final Document doc = new Document()
                .append("cpu_usage", cpuUsage)
                .append("ram_usage", memoryUsed)
                .append("thread_count", threadCount)
                .append("timestamp", Instant.now().toEpochMilli());
        return Mono.from(this.getCollection()
                .updateOne(Filters.eq("_id", "system_resources"),
                        Updates.pushEach("system_resources", List.of(doc), new PushOptions().slice(-slice)),
                        new UpdateOptions().upsert(true)))
                .doOnNext(result -> LOGGER.debug("[System stats] Logging system resources stats result: {}", result));
    }

    public Mono<TotalCommandStats> getCommandStats() {
        LOGGER.debug("[Command stats] Request");

        final Publisher<Document> request = this.getCollection().find(new Document().append("_id", "command_stats"));

        return Mono.from(request)
                .map(document -> document.toJson(Utils.JSON_WRITER_SETTINGS))
                .flatMap(json -> Mono.fromCallable(() -> Utils.MAPPER.readValue(json, TotalCommandStatsBean.class)))
                .map(TotalCommandStats::new);
    }

    public Mono<DailyResourceStats> getResourcesStats() {
        LOGGER.debug("[System stats] Request");

        final Publisher<Document> request = this.getCollection().find(new Document().append("_id", "system_resources"));

        return Mono.from(request)
                .map(document -> document.toJson(Utils.JSON_WRITER_SETTINGS))
                .flatMap(json -> Mono.fromCallable(() -> Utils.MAPPER.readValue(json, DailyResourceStatsBean.class)))
                .map(DailyResourceStats::new);
    }
}
