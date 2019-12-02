package com.shadorc.shadbot.db;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.guilds.GuildsCollection;
import com.shadorc.shadbot.db.lottery.LotteryCollection;
import com.shadorc.shadbot.db.premium.PremiumCollection;
import com.shadorc.shadbot.db.serializer.SnowflakeSerializer;
import discord4j.core.object.util.Snowflake;

public class DatabaseManager {

    private static DatabaseManager instance;

    static {
        DatabaseManager.instance = new DatabaseManager();
    }

    private final MongoClient client;
    private final MongoDatabase database;
    private final Gson gson;

    private final PremiumCollection premiumCollection;
    private final GuildsCollection guildsCollection;
    private final LotteryCollection lotteryCollection;

    private DatabaseManager() {
        this.client = MongoClients.create();
        this.database = this.client.getDatabase(Config.DATABASE_NAME);

        this.premiumCollection = new PremiumCollection(this.database);
        this.guildsCollection = new GuildsCollection(this.database);
        this.lotteryCollection = new LotteryCollection(this.database);

        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Snowflake.class, new SnowflakeSerializer());
        this.gson = gsonBuilder.create();
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

    public Gson getGson() {
        return this.gson;
    }

    public void close() {
        this.client.close();
    }

    public static DatabaseManager getInstance() {
        return DatabaseManager.instance;
    }

}
