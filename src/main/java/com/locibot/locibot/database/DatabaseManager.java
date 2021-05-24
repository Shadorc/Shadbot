package com.locibot.locibot.database;

import com.locibot.locibot.database.codec.IamCodec;
import com.locibot.locibot.database.codec.LongCodec;
import com.locibot.locibot.database.codec.SnowflakeCodec;
import com.locibot.locibot.database.groups.GroupsCollection;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.data.credential.Credential;
import com.locibot.locibot.data.credential.CredentialManager;
import com.locibot.locibot.database.codec.AutoMessageCodec;
import com.locibot.locibot.database.guilds.GuildsCollection;
import com.locibot.locibot.database.lottery.LotteryCollection;
import com.locibot.locibot.database.premium.PremiumCollection;
import com.locibot.locibot.database.users.UsersCollection;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

public class DatabaseManager {

    protected static final CodecRegistry CODEC_REGISTRY = CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromCodecs(new SnowflakeCodec(), new LongCodec(), new IamCodec(), new AutoMessageCodec()));

    private static final MongoClient CLIENT;
    private static final PremiumCollection PREMIUM_COLLECTION;
    private static final GuildsCollection GUILDS_COLLECTION;
    private static final LotteryCollection LOTTERY_COLLECTION;
    private static final UsersCollection USERS_COLLECTION;
    private static final GroupsCollection GROUPS_COLLECTION;

    static {
        final MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
                .codecRegistry(CODEC_REGISTRY)
                .applicationName("Shadbot V%s".formatted(Config.VERSION));

        if (!Config.IS_SNAPSHOT) {
            final String username = CredentialManager.get(Credential.DATABASE_USERNAME);
            final String pwd = CredentialManager.get(Credential.DATABASE_PWD);
            final String host = CredentialManager.get(Credential.DATABASE_HOST);
            final String port = CredentialManager.get(Credential.DATABASE_PORT);
            if (username != null && pwd != null && host != null && port != null) {
                settingsBuilder.applyConnectionString(new ConnectionString(
                        //"mongodb://%s:%s@%s:%s/%s".formatted(username, pwd, host, port, Config.DATABASE_NAME)));
                        "mongodb+srv://%s:%s@%s/%s".formatted(username, pwd, host, Config.DATABASE_NAME)));
            }
        }

        CLIENT = MongoClients.create(settingsBuilder.build());

        final MongoDatabase database = CLIENT.getDatabase(Config.DATABASE_NAME);
        PREMIUM_COLLECTION = new PremiumCollection(database);
        GUILDS_COLLECTION = new GuildsCollection(database);
        LOTTERY_COLLECTION = new LotteryCollection(database);
        USERS_COLLECTION = new UsersCollection(database);
        GROUPS_COLLECTION = new GroupsCollection(database);
    }

    public static PremiumCollection getPremium() {
        return PREMIUM_COLLECTION;
    }

    public static GuildsCollection getGuilds() {
        return GUILDS_COLLECTION;
    }

    public static LotteryCollection getLottery() {
        return LOTTERY_COLLECTION;
    }

    public static UsersCollection getUsers() {
        return USERS_COLLECTION;
    }

    public static GroupsCollection getGroups() {
        return GROUPS_COLLECTION;
    }

    public static void close() {
        CLIENT.close();
    }

}
