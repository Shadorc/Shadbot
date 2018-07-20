package me.shadorc.shadbot.data.premium;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.JavaType;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.data.DataManager;
import me.shadorc.shadbot.data.annotation.DataInit;
import me.shadorc.shadbot.data.annotation.DataSave;
import me.shadorc.shadbot.data.premium.Relic.RelicType;
import me.shadorc.shadbot.exception.RelicActivationException;
import me.shadorc.shadbot.utils.Utils;

public class PremiumManager {

	private static final String FILE_NAME = "premium_data.json";
	private static final File FILE = new File(DataManager.SAVE_DIR, FILE_NAME);

	private static List<Relic> relics;

	@DataInit
	public static void init() throws IOException {
		final JavaType valueType = Utils.MAPPER.getTypeFactory().constructCollectionType(CopyOnWriteArrayList.class, Relic.class);
		relics = FILE.exists() ? Utils.MAPPER.readValue(FILE, valueType) : new CopyOnWriteArrayList<>();
	}

	@DataSave(filePath = FILE_NAME, initialDelay = 1, period = 1, unit = TimeUnit.HOURS)
	public static void save() throws IOException {
		try (FileWriter writer = new FileWriter(FILE)) {
			writer.write(Utils.MAPPER.writeValueAsString(relics));
		}
	}

	public static Relic generateRelic(RelicType type) {
		Relic relic = new Relic(UUID.randomUUID().toString(), TimeUnit.DAYS.toMillis(180), type);
		relics.add(relic);
		return relic;
	}

	public static void activateRelic(@Nullable Snowflake guildId, Snowflake userId, String relicId) throws RelicActivationException {
		Optional<Relic> relicOpt = relics.stream()
				.filter(relicItr -> relicItr.getId().equals(relicId))
				.findFirst();

		if(!relicOpt.isPresent()) {
			throw new RelicActivationException("This key is already activated or doesn't exist.");
		}

		final Relic relic = relicOpt.get();

		if(relic.getType().equals(RelicType.GUILD.toString()) && guildId == null) {
			throw new RelicActivationException("You must activate a Legendary Relic in the desired server.");
		}

		relic.activate(userId);

		if(relic.getType().equals(RelicType.GUILD.toString())) {
			relic.setGuildId(guildId);
		}
	}

	public static List<Relic> getRelicsForUser(Snowflake userId) {
		return relics.stream()
				.filter(relic -> relic.getUserId().isPresent())
				.filter(relic -> relic.getUserId().get().equals(userId))
				.collect(Collectors.toList());
	}

	public static boolean isPremium(Snowflake guildId, Snowflake userId) {
		return PremiumManager.isGuildPremium(guildId) || PremiumManager.isUserPremium(userId);
	}

	public static boolean isGuildPremium(Snowflake guildId) {
		return relics.stream()
				.filter(relic -> relic.getGuildId().isPresent())
				.filter(relic -> relic.getGuildId().get().equals(guildId))
				.anyMatch(relic -> !relic.isExpired());
	}

	public static boolean isUserPremium(Snowflake userId) {
		return PremiumManager.getRelicsForUser(userId).stream()
				.anyMatch(relic -> PremiumManager.isValid(relic, RelicType.USER));
	}

	private static boolean isValid(Relic relic, RelicType type) {
		return relic.getType().equals(type.toString()) && !relic.isExpired();
	}

}
