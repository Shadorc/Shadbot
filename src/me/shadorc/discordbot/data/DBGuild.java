package me.shadorc.discordbot.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import sx.blah.discord.handle.obj.IGuild;

public class DBGuild {

	public static final String SETTINGS = "settings";
	public static final String USERS = "users";

	private final ConcurrentHashMap<Setting, Object> settingsMap;
	private final ConcurrentHashMap<Long, DBUser> usersMap;

	private final IGuild guild;

	public DBGuild(IGuild guild, JSONObject guildObj) {
		this.settingsMap = new ConcurrentHashMap<>();
		this.usersMap = new ConcurrentHashMap<>();
		this.guild = guild;

		JSONObject settingsObj = guildObj.optJSONObject(DBGuild.SETTINGS);
		if(settingsObj != null) {
			Arrays.stream(Setting.values())
					.filter(setting -> setting.isSaveable() && settingsObj.has(setting.toString()))
					.forEach(setting -> settingsMap.put(setting, settingsObj.get(setting.toString())));
		}

		JSONArray usersArray = guildObj.optJSONArray(DBGuild.USERS);
		if(usersArray != null) {
			for(int i = 0; i < usersArray.length(); i++) {
				DBUser user = new DBUser(guild, usersArray.getJSONObject(i));
				usersMap.put(user.getUser().getLongID(), user);
			}
		}
	}

	public DBGuild(IGuild guild) {
		this(guild, new JSONObject());
	}

	public IGuild getGuild() {
		return guild;
	}

	public DBUser getUser(long userID) {
		return usersMap.getOrDefault(userID, new DBUser(guild, userID));
	}

	public Collection<DBUser> getUsers() {
		return usersMap.values();
	}

	public Object getSetting(Setting setting) {
		Object settingObj = settingsMap.get(setting);
		if(settingObj == null) {
			switch (setting) {
				case ALLOWED_CHANNELS:
				case BLACKLIST:
					return new JSONArray();
				case DEFAULT_VOLUME:
					return Config.DEFAULT_VOLUME;
				case PREFIX:
					return Config.DEFAULT_PREFIX;
				default:
					return null;
			}
		}
		return settingObj;
	}

	public void setSetting(Setting setting, Object value) {
		settingsMap.put(setting, value);
		Storage.saveGuild(this);
	}

	public void removeSetting(Setting setting) {
		settingsMap.remove(setting);
		Storage.saveGuild(this);
	}

	public void saveUser(DBUser user) {
		usersMap.put(user.getUser().getLongID(), user);
		Storage.saveGuild(this);
	}

	public JSONObject toJSON() {
		JSONObject guildObj = new JSONObject();

		JSONObject settingsObj = new JSONObject();
		settingsMap.keySet().stream().forEach(setting -> settingsObj.put(setting.toString(), settingsMap.get(setting)));
		guildObj.put(DBGuild.SETTINGS, settingsObj);

		JSONArray usersArray = new JSONArray();
		usersMap.values().stream().forEach(user -> usersArray.put(user.toJSON()));
		guildObj.put(DBGuild.USERS, usersArray);

		return guildObj;
	}
}