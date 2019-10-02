package com.shadorc.shadbot.db.premium;

import com.fasterxml.jackson.databind.JavaType;
import com.shadorc.shadbot.db.DatabaseTable;
import com.shadorc.shadbot.db.premium.Relic.RelicType;
import com.shadorc.shadbot.utils.ExitCode;
import com.shadorc.shadbot.utils.LogUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.util.Snowflake;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PremiumManager extends DatabaseTable {

    private static PremiumManager instance;

    static {
        try {
            PremiumManager.instance = new PremiumManager();
        } catch (final Exception err) {
            LogUtils.error(err, String.format("An error occurred while initializing %s.", PremiumManager.class.getSimpleName()));
            System.exit(ExitCode.FATAL_ERROR.getValue());
        }
    }

    private final List<Relic> relics;

    private PremiumManager() throws IOException {
        super("premium");

        this.relics = new CopyOnWriteArrayList<>();

        final String premiumJson = this.getTable().toJson().run(this.getConnection());
        if (premiumJson != null) {
            final JavaType valueType = Utils.MAPPER.getTypeFactory().constructCollectionType(List.class, Relic.class);
            this.relics.addAll(Utils.MAPPER.readValue(premiumJson, valueType));
        }
    }

    public Relic generateRelic(RelicType type) {
        final Relic relic = new Relic(UUID.randomUUID().toString(), TimeUnit.DAYS.toMillis(180), type);
        this.relics.add(relic);
        return relic;
    }

    public Optional<Relic> getRelic(String id) {
        return this.relics.stream()
                .filter(relic -> relic.getId().equals(id))
                .findFirst();
    }

    public List<Relic> getRelicsForUser(Snowflake userId) {
        return this.relics.stream()
                .filter(relic -> relic.getUserId().map(userId::equals).orElse(false))
                .collect(Collectors.toList());
    }

    public boolean isGuildPremium(Snowflake guildId) {
        return this.relics.stream()
                .filter(relic -> relic.getGuildId().map(guildId::equals).orElse(false))
                .anyMatch(relic -> !relic.isExpired());
    }

    public boolean isUserPremium(Snowflake userId) {
        return this.getRelicsForUser(userId).stream()
                .anyMatch(relic -> relic.getType().equals(RelicType.USER.toString()) && !relic.isExpired());
    }

    public static PremiumManager getInstance() {
        return PremiumManager.instance;
    }

}
