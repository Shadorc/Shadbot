package com.shadorc.shadbot.db.lottery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.shadorc.shadbot.db.DatabaseCollection;
import com.shadorc.shadbot.db.lottery.bean.LotteryGamblerBean;
import com.shadorc.shadbot.db.lottery.bean.LotteryHistoricBean;
import com.shadorc.shadbot.db.lottery.entity.LotteryGambler;
import com.shadorc.shadbot.db.lottery.entity.LotteryHistoric;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.util.Snowflake;
import org.bson.Document;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class LotteryCollection extends DatabaseCollection {

    public static final Logger LOGGER = Loggers.getLogger("shadbot.database.lottery");

    public LotteryCollection(MongoDatabase database) {
        super(database.getCollection("lottery"));
    }

    public List<LotteryGambler> getGamblers() {
        LOGGER.debug("[Lottery gamblers] Request.");

        final Document document = this.getCollection()
                .find(Filters.eq("_id", "gamblers"))
                .first();

        if (document == null) {
            LOGGER.debug("[Lottery gamblers] Not found.");
            return Collections.emptyList();
        } else {
            LOGGER.debug("[Lottery gamblers] Found.");
            return document.getList("gamblers", Document.class)
                    .stream()
                    .map(Document::toJson)
                    .map(json -> {
                        try {
                            return Utils.MAPPER.readValue(json, LotteryGamblerBean.class);
                        } catch (final JsonProcessingException err) {
                            throw new RuntimeException(err);
                        }
                    })
                    .map(LotteryGambler::new)
                    .collect(Collectors.toUnmodifiableList());
        }
    }

    public Optional<LotteryHistoric> getHistoric() {
        LOGGER.debug("[Lottery historic] Request.");

        final Document document = this.getCollection()
                .find(Filters.eq("_id", "historic"))
                .first();

        if (document == null) {
            LOGGER.debug("[Lottery historic] Not found.");
            return Optional.empty();
        } else {
            LOGGER.debug("[Lottery historic] Found.");
            return Optional.of(document)
                    .map(Document::toJson)
                    .map(json -> {
                        try {
                            return Utils.MAPPER.readValue(json, LotteryHistoricBean.class);
                        } catch (final JsonProcessingException err) {
                            throw new RuntimeException(err);
                        }
                    })
                    .map(LotteryHistoric::new);
        }
    }

    public long getJackpot() {
        LOGGER.debug("[Lottery jackpot] Request.");

        final Document document = this.getCollection()
                .find(Filters.eq("_id", "jackpot"))
                .first();

        if (document == null) {
            LOGGER.debug("[Lottery jackpot] Not found.");
            return 0;
        } else {
            LOGGER.debug("[Lottery jackpot] Found.");
            return document.getLong("jackpot");
        }
    }

    public boolean isGambler(Snowflake userId) {
        LOGGER.debug("[Gambler {}] Checking if user exist.", userId.asLong());

        final Document document = this.getCollection()
                .find(Filters.and(Filters.eq("_id", "gamblers"),
                        Filters.eq("gamblers.user_id", userId.asString())))
                .first();

        return document != null;
    }

    public void resetGamblers() {
        LOGGER.debug("[Lottery gamblers] Reset.");

        this.getCollection()
                .deleteOne(Filters.eq("_id", "gamblers"));
    }

    public void addToJackpot(long coins) {
        final long value = (long) Math.ceil(coins / 100.0f);

        LOGGER.debug("[Lottery jackpot] Adding {}.", FormatUtils.coins(value));

        this.getCollection()
                .updateOne(Filters.eq("_id", "jackpot"),
                        Updates.inc("jackpot", value),
                        new UpdateOptions().upsert(true));
    }

    public void resetJackpot() {
        LOGGER.debug("[Lottery jackpot] Reset.");

        this.getCollection()
                .deleteOne(Filters.eq("_id", "jackpot"));
    }

}
