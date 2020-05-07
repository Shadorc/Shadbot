package com.shadorc.shadbot.db;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.db.guilds.GuildsCollection;
import com.shadorc.shadbot.db.lottery.LotteryCollection;
import com.shadorc.shadbot.db.premium.PremiumCollection;
import com.shadorc.shadbot.utils.Utils;
import io.prometheus.client.Counter;

public class DatabaseManager {

    public static final Counter DB_REQUEST_COUNTER = Counter.build().namespace("database")
            .name("request_count").help("Database request count").labelNames("collection").register();

    private static DatabaseManager instance;

    static {
        DatabaseManager.instance = new DatabaseManager();
    }

    private final MongoClient client;

    private final PremiumCollection premiumCollection;
    private final GuildsCollection guildsCollection;
    private final LotteryCollection lotteryCollection;

    private DatabaseManager() {
        final MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
                .codecRegistry(Utils.CODEC_REGISTRY)
                .applicationName(String.format("Shadbot V%s", Config.VERSION));

        if (!Config.IS_SNAPSHOT) {
            final String username = CredentialManager.getInstance().get(Credential.DATABASE_USERNAME);
            final String pwd = CredentialManager.getInstance().get(Credential.DATABASE_PWD);
            final String host = CredentialManager.getInstance().get(Credential.DATABASE_HOST);
            final String port = CredentialManager.getInstance().get(Credential.DATABASE_PORT);
            if (username != null && pwd != null && host != null && port != null) {
                settingsBuilder.applyConnectionString(new ConnectionString(
                        String.format("mongodb://%s:%s@%s:%s/%s", username, pwd, host, port, Config.DATABASE_NAME)));
            }
        }

        this.client = MongoClients.create(settingsBuilder.build());

        final MongoDatabase database = this.client.getDatabase(Config.DATABASE_NAME);
        this.premiumCollection = new PremiumCollection(database);
        this.guildsCollection = new GuildsCollection(database);
        this.lotteryCollection = new LotteryCollection(database);
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

    public void close() {
        this.client.close();
    }

    public static DatabaseManager getInstance() {
        return DatabaseManager.instance;
    }

}
