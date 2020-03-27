package com.shadorc.shadbot.db;

import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.guilds.GuildsCollection;
import com.shadorc.shadbot.db.lottery.LotteryCollection;
import com.shadorc.shadbot.db.premium.PremiumCollection;
import com.shadorc.shadbot.db.stats.StatsCollection;
import com.shadorc.shadbot.utils.Utils;

public class DatabaseManager {

    private static DatabaseManager instance;

    static {
        DatabaseManager.instance = new DatabaseManager();
    }

    private final MongoClient client;

    private final PremiumCollection premiumCollection;
    private final GuildsCollection guildsCollection;
    private final LotteryCollection lotteryCollection;
    private final StatsCollection statsCollection;

    private DatabaseManager() {
        final MongoClientSettings settings = MongoClientSettings.builder()
                .codecRegistry(Utils.CODEC_REGISTRY)
                .build();

        this.client = MongoClients.create(settings);

        final MongoDatabase database = this.client.getDatabase(Config.DATABASE_NAME);
        this.premiumCollection = new PremiumCollection(database);
        this.guildsCollection = new GuildsCollection(database);
        this.lotteryCollection = new LotteryCollection(database);
        this.statsCollection = new StatsCollection(database);
    }

    public static PremiumCollection getPremium() {
        return DatabaseManager.instance.premiumCollection;
    }

    public static GuildsCollection getGuilds() {
        return DatabaseManager.instance.guildsCollection;
    }

    public static LotteryCollection getLottery() {
        return DatabaseManager.instance.lotteryCollection;
    }

    public static StatsCollection getStats() {
        return DatabaseManager.instance.statsCollection;
    }

    public void close() {
        this.client.close();
    }

    public static DatabaseManager getInstance() {
        return DatabaseManager.instance;
    }

}
