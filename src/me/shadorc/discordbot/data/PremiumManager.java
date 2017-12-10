package me.shadorc.discordbot.data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import me.shadorc.discordbot.exceptions.RelicActivationException;
import me.shadorc.discordbot.utils.LogUtils;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class PremiumManager {

	private static final File PREMIUM_DATA_FILE = new File("premium_data.json");

	public enum RelicType {
		USER, GUILD;
	}

	@SuppressWarnings("ucd")
	private static JSONObject dataObj;

	static {
		if(!PREMIUM_DATA_FILE.exists()) {
			try (FileWriter writer = new FileWriter(PREMIUM_DATA_FILE)) {
				JSONObject defaultObj = new JSONObject();
				defaultObj.put(JSONKey.DONATORS.toString(), new JSONObject());
				defaultObj.put(JSONKey.UNUSED_RELICS.toString(), new JSONArray());

				writer.write(defaultObj.toString(Config.INDENT_FACTOR));
				writer.flush();

			} catch (IOException err) {
				LogUtils.LOGGER.error("An error occurred during premium data file creation. Exiting.", err);
				System.exit(1);
			}
		}

		try (InputStream stream = PREMIUM_DATA_FILE.toURI().toURL().openStream()) {
			dataObj = new JSONObject(new JSONTokener(stream));
		} catch (JSONException | IOException err) {
			LogUtils.LOGGER.error("An error occurred during premium data file initialisation. Exiting.", err);
			System.exit(1);
		}
	}

	public static JSONObject generateRelic(RelicType type) {
		JSONObject relicObj = new JSONObject();
		relicObj.put(JSONKey.RELIC_ID.toString(), UUID.randomUUID().toString());
		relicObj.put(JSONKey.RELIC_DURATION.toString(), 180);
		relicObj.put(JSONKey.RELIC_EXPIRED.toString(), false);
		relicObj.put(JSONKey.RELIC_TYPE.toString(), type.toString());

		JSONArray unusedKeys = dataObj.getJSONArray(JSONKey.UNUSED_RELICS.toString());
		unusedKeys.put(relicObj);

		dataObj.put(JSONKey.UNUSED_RELICS.toString(), unusedKeys);
		PremiumManager.save();
		return relicObj;
	}

	public static void activateRelic(IGuild guild, IUser user, String relicID) throws RelicActivationException {
		JSONObject relicObj = PremiumManager.getUnusedRelic(relicID);

		if(relicObj == null) {
			throw new RelicActivationException("This key is already activated or doesn't exist.");
		}

		boolean isLegendaryRelic = relicObj.getString(JSONKey.RELIC_TYPE.toString()).equals(RelicType.GUILD.toString());
		if(isLegendaryRelic && guild == null) {
			throw new RelicActivationException("You must activate a Legendary Relic in the desired server.");
		}

		relicObj.put(JSONKey.RELIC_ACTIVATION_MILLIS.toString(), System.currentTimeMillis());
		if(isLegendaryRelic) {
			relicObj.put(JSONKey.GUILD_ID.toString(), guild.getLongID());
		}

		JSONArray userKeys = dataObj.getJSONObject(JSONKey.DONATORS.toString()).optJSONArray(user.getStringID());
		if(userKeys == null) {
			userKeys = new JSONArray();
		}

		userKeys.put(relicObj);

		dataObj.getJSONObject(JSONKey.DONATORS.toString()).put(user.getStringID(), userKeys);
		dataObj.getJSONArray(JSONKey.UNUSED_RELICS.toString()).remove(PremiumManager.getUnusedRelicIndex(relicID));
		PremiumManager.save();
	}

	private static JSONObject getUnusedRelic(String relicID) {
		JSONArray unusedRelics = dataObj.getJSONArray(JSONKey.UNUSED_RELICS.toString());
		for(int i = 0; i < unusedRelics.length(); i++) {
			JSONObject relicObj = unusedRelics.getJSONObject(i);
			if(relicObj.getString(JSONKey.RELIC_ID.toString()).equals(relicID)) {
				return relicObj;
			}
		}
		return null;
	}

	private static int getUnusedRelicIndex(String relicID) {
		JSONArray unusedRelics = dataObj.getJSONArray(JSONKey.UNUSED_RELICS.toString());
		for(int i = 0; i < unusedRelics.length(); i++) {
			if(unusedRelics.getJSONObject(i).getString(JSONKey.RELIC_ID.toString()).equals(relicID)) {
				return i;
			}
		}
		return -1;
	}

	public static boolean isGuildPremium(IGuild guild) {
		JSONObject donatorsObj = dataObj.getJSONObject(JSONKey.DONATORS.toString());
		for(Object userKey : donatorsObj.keySet()) {
			JSONArray keysArray = donatorsObj.getJSONArray(userKey.toString());
			for(int i = 0; i < keysArray.length(); i++) {
				JSONObject keyObj = keysArray.getJSONObject(i);
				if(PremiumManager.isValid(keyObj, RelicType.GUILD)
						&& keyObj.optLong(JSONKey.GUILD_ID.toString()) == guild.getLongID()) {
					return true;
				}
			}
		}

		return false;
	}

	public static boolean isUserPremium(IUser user) {
		JSONArray keysArray = PremiumManager.getKeysForUser(user.getLongID());
		if(keysArray == null) {
			return false;
		}

		for(int i = 0; i < keysArray.length(); i++) {
			JSONObject keyObj = keysArray.getJSONObject(i);
			if(PremiumManager.isValid(keyObj, RelicType.USER)) {
				return true;
			}
		}

		return false;
	}

	private static boolean isValid(JSONObject keyObj, RelicType type) {
		// Wrong type
		if(!keyObj.getString(JSONKey.RELIC_TYPE.toString()).equals(type.toString())) {
			return false;
		}

		// Is activated
		if(keyObj.optLong(JSONKey.RELIC_ACTIVATION_MILLIS.toString()) == 0) {
			return false;
		}

		// Update expiration
		if(TimeUnit.MILLISECONDS.toDays(keyObj.getLong(JSONKey.RELIC_ACTIVATION_MILLIS.toString()) + System.currentTimeMillis()) > keyObj.getInt(JSONKey.RELIC_DURATION.toString())) {
			keyObj.put(JSONKey.RELIC_EXPIRED.toString(), true);
		}

		// Expired
		return keyObj.getBoolean(JSONKey.RELIC_EXPIRED.toString());
	}

	public static JSONArray getKeysForUser(long userID) {
		JSONObject donatorsObj = dataObj.getJSONObject(JSONKey.DONATORS.toString());
		for(Object key : donatorsObj.keySet()) {
			if(Long.parseLong(key.toString()) == userID) {
				return donatorsObj.getJSONArray(key.toString());
			}
		}
		return null;
	}

	private synchronized static void save() {
		LogUtils.info("Saving premium data...");
		try (FileWriter writer = new FileWriter(PREMIUM_DATA_FILE)) {
			writer.write(dataObj.toString(Config.INDENT_FACTOR));
			writer.flush();

		} catch (IOException err) {
			LogUtils.error("Error while saving premium data.", err);
		}
		LogUtils.info("Premium data saved.");
	}

}
