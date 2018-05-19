package me.shadorc.shadbot.data.premium;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.data.DataManager;
import me.shadorc.shadbot.data.annotation.DataInit;
import me.shadorc.shadbot.data.annotation.DataSave;
import me.shadorc.shadbot.exception.RelicActivationException;
import me.shadorc.shadbot.utils.Utils;

public class PremiumManager {

	private static final String DONATORS = "donators";
	private static final String UNUSED_RELICS = "unusedRelics";

	private static final String FILE_NAME = "premium_data.json";
	private static final File FILE = new File(DataManager.SAVE_DIR, FILE_NAME);

	private static final Map<Snowflake, Relic> UNUSED_RELICS_MAP = new HashMap<>();

	private static JSONObject premiumObj;

	@DataInit
	public static void init() throws JSONException, IOException {
		if(!FILE.exists()) {
			try (FileWriter writer = new FileWriter(FILE)) {
				JSONObject defaultObj = new JSONObject()
						.put(DONATORS, new JSONObject())
						.put(UNUSED_RELICS, new JSONArray());

				writer.write(defaultObj.toString(Config.JSON_INDENT_FACTOR));
			}
		}

		try (InputStream stream = FILE.toURI().toURL().openStream()) {
			premiumObj = new JSONObject(new JSONTokener(stream));
		}

		Utils.toList(premiumObj.getJSONArray(UNUSED_RELICS), JSONObject.class)
				.stream()
				.map(Relic::new)
				.forEach(relic -> UNUSED_RELICS_MAP.put(relic.getId(), relic));
	}

	@DataSave(filePath = FILE_NAME, initialDelay = 1, period = 1, unit = TimeUnit.HOURS)
	public static void save() throws JSONException, IOException {
		premiumObj.put(UNUSED_RELICS, new JSONArray(UNUSED_RELICS_MAP.values()));
		try (FileWriter writer = new FileWriter(FILE)) {
			writer.write(premiumObj.toString(Config.JSON_INDENT_FACTOR));
		}
	}

	public static Relic generateRelic(RelicType type) {
		Relic relic = new Relic(Snowflake.of(UUID.randomUUID().toString()), 180, type);
		UNUSED_RELICS_MAP.put(relic.getId(), relic);
		return relic;
	}

	public static void activateRelic(Optional<Snowflake> guildId, Snowflake userId, Snowflake relicId) throws RelicActivationException {
		if(!UNUSED_RELICS_MAP.containsKey(relicId)) {
			throw new RelicActivationException("This key is already activated or doesn't exist.");
		}

		Relic relic = UNUSED_RELICS_MAP.get(relicId);
		if(relic.getType().equals(RelicType.GUILD) && !guildId.isPresent()) {
			throw new RelicActivationException("You must activate a Legendary Relic in the desired server.");
		}

		relic.activate();
		if(relic.getType().equals(RelicType.GUILD)) {
			relic.setGuildId(guildId.get());
		}

		JSONArray userKeys = premiumObj.getJSONObject(DONATORS).optJSONArray(userId.asString());
		if(userKeys == null) {
			userKeys = new JSONArray();
		}

		userKeys.put(relic.toJSON());
		UNUSED_RELICS_MAP.remove(relic.getId());

		premiumObj.getJSONObject(DONATORS).put(userId.asString(), userKeys);
	}

	public static List<Relic> getRelicsForUser(User user) {
		List<Relic> relics = new ArrayList<>();
		JSONObject donatorsObj = premiumObj.getJSONObject(DONATORS);
		JSONArray donatorRelics = donatorsObj.optJSONArray(user.getId().asString());
		if(donatorRelics != null) {
			donatorRelics.forEach(relicObj -> relics.add(new Relic((JSONObject) relicObj)));
		}
		return relics;
	}

	public static boolean isPremium(Guild guild, User user) {
		return PremiumManager.isPremium(guild) || PremiumManager.isPremium(user);
	}

	public static boolean isPremium(Guild guild) {
		JSONObject donatorsObj = premiumObj.getJSONObject(DONATORS);
		for(String userKey : donatorsObj.keySet()) {
			JSONArray keysArray = donatorsObj.getJSONArray(userKey);
			for(int i = 0; i < keysArray.length(); i++) {
				Relic relic = new Relic(keysArray.getJSONObject(i));
				if(PremiumManager.isValid(relic, RelicType.GUILD) && relic.getGuildId().equals(guild.getId())) {
					return true;
				}
			}
		}

		return false;
	}

	public static boolean isPremium(User user) {
		List<Relic> relics = PremiumManager.getRelicsForUser(user);
		if(relics.isEmpty()) {
			return false;
		}

		return relics.stream().anyMatch(relic -> PremiumManager.isValid(relic, RelicType.USER));
	}

	private static boolean isValid(Relic relic, RelicType type) {
		return relic.getType().equals(type) && relic.getActivationTime() != 0 && !relic.isExpired();
	}

}
