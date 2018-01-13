package me.shadorc.shadbot.data.premium;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.data.annotation.DataInit;
import me.shadorc.shadbot.data.annotation.DataSave;
import me.shadorc.shadbot.exception.RelicActivationException;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class PremiumManager {

	private static final String DONATORS = "donators";
	private static final String UNUSED_RELICS = "unusedRelics";

	private static final String FILE_NAME = "premium_data.json";
	private static final File FILE = new File(FILE_NAME);

	private static final List<Relic> UNUSED_RELICS_LIST = new ArrayList<>();

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

		premiumObj.getJSONArray(UNUSED_RELICS)
				.forEach(relicObj -> UNUSED_RELICS_LIST.add(new Relic((JSONObject) relicObj)));
	}

	@DataSave(filePath = FILE_NAME, initialDelay = 1, period = 1, unit = TimeUnit.HOURS)
	public static void save() throws JSONException, IOException {
		premiumObj.put(UNUSED_RELICS, new JSONArray(UNUSED_RELICS_LIST));
		try (FileWriter writer = new FileWriter(FILE)) {
			writer.write(premiumObj.toString(Config.JSON_INDENT_FACTOR));
		}
	}

	public static Relic generateRelic(RelicType type) {
		Relic relic = new Relic(UUID.randomUUID().toString(), 180, type);
		UNUSED_RELICS_LIST.add(relic);
		return relic;
	}

	public static void activateRelic(IGuild guild, IUser user, String relicID) throws RelicActivationException {
		Relic relic = UNUSED_RELICS_LIST.stream().filter(unusedRelic -> unusedRelic.getID().equals(relicID)).findAny().get();

		if(relic == null) {
			throw new RelicActivationException("This key is already activated or doesn't exist.");
		}

		if(relic.getType().equals(RelicType.GUILD) && guild == null) {
			throw new RelicActivationException("You must activate a Legendary Relic in the desired server.");
		}

		relic.activate();
		if(relic.getType().equals(RelicType.GUILD)) {
			relic.setGuildID(guild.getLongID());
		}

		JSONArray userKeys = premiumObj.getJSONObject(DONATORS).optJSONArray(user.getStringID());
		if(userKeys == null) {
			userKeys = new JSONArray();
		}

		userKeys.put(relic.toJSON());
		UNUSED_RELICS_LIST.remove(relic);

		premiumObj.getJSONObject(DONATORS).put(user.getStringID(), userKeys);
	}

	public static List<Relic> getRelicsForUser(long userID) {
		List<Relic> relics = new ArrayList<>();
		JSONObject donatorsObj = premiumObj.getJSONObject(DONATORS);
		JSONArray donatorRelics = donatorsObj.optJSONArray(Long.toString(userID));
		if(donatorRelics != null) {
			donatorRelics.forEach(relicObj -> relics.add(new Relic((JSONObject) relicObj)));
		}
		return relics;
	}

	public static boolean isPremium(IGuild guild, IUser user) {
		return PremiumManager.isPremium(guild) || PremiumManager.isPremium(user);
	}

	public static boolean isPremium(IGuild guild) {
		JSONObject donatorsObj = premiumObj.getJSONObject(DONATORS);
		for(String userKey : donatorsObj.keySet()) {
			JSONArray keysArray = donatorsObj.getJSONArray(userKey);
			for(int i = 0; i < keysArray.length(); i++) {
				Relic relic = new Relic(keysArray.getJSONObject(i));
				if(PremiumManager.isValid(relic, RelicType.GUILD) && relic.getGuildID() == guild.getLongID()) {
					return true;
				}
			}
		}

		return false;
	}

	public static boolean isPremium(IUser user) {
		List<Relic> relics = PremiumManager.getRelicsForUser(user.getLongID());
		if(relics.isEmpty()) {
			return false;
		}

		return relics.stream().anyMatch(relic -> PremiumManager.isValid(relic, RelicType.USER));
	}

	private static boolean isValid(Relic relic, RelicType type) {
		return relic.getType().equals(type) && relic.getActivationTime() != 0 && !relic.isExpired();
	}

}
