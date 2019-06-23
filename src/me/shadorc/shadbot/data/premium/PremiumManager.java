package me.shadorc.shadbot.data.premium;

import com.fasterxml.jackson.databind.JavaType;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.data.Data;
import me.shadorc.shadbot.data.premium.Relic.RelicType;
import me.shadorc.shadbot.utils.ExitCode;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PremiumManager extends Data {

    private static PremiumManager instance;

    static {
        try {
            PremiumManager.instance = new PremiumManager();
        } catch (final IOException err) {
            LogUtils.error(err, String.format("An error occurred while initializing %s.", PremiumManager.class.getSimpleName()));
            System.exit(ExitCode.FATAL_ERROR.value());
        }
    }

    private final List<Relic> relics;

    private PremiumManager() throws IOException {
        super("premium_data.json", Duration.ofHours(1), Duration.ofHours(1));

        this.relics = new CopyOnWriteArrayList<>();
        if (this.getFile().exists()) {
            final JavaType valueType = Utils.MAPPER.getTypeFactory().constructCollectionType(List.class, Relic.class);
            this.relics.addAll(Utils.MAPPER.readValue(this.getFile(), valueType));
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

    public boolean isPremium(Snowflake guildId, Snowflake userId) {
        return this.isGuildPremium(guildId) || this.isUserPremium(userId);
    }

    private boolean isGuildPremium(Snowflake guildId) {
        return this.relics.stream()
                .filter(relic -> relic.getGuildId().map(guildId::equals).orElse(false))
                .anyMatch(relic -> !relic.isExpired());
    }

    private boolean isUserPremium(Snowflake userId) {
        return this.getRelicsForUser(userId).stream()
                .anyMatch(relic -> relic.getType().equals(RelicType.USER.toString()) && !relic.isExpired());
    }

    @Override
    public Object getData() {
        return Collections.unmodifiableList(this.relics);
    }

    public static PremiumManager getInstance() {
        return PremiumManager.instance;
    }

}
