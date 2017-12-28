package me.shadorc.shadbot.data.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.data.Setting;
import me.shadorc.shadbot.data.Database;
import me.shadorc.shadbot.utils.JSONUtils;
import sx.blah.discord.handle.obj.IGuild;

public class DBGuild {

	public static final String USERS_KEY = "users";
	private static final String SETTINGS_KEY = "settings";

	private final IGuild guild;
	private final Map<Setting, Object> settingsMap;
	private final Map<Long, DBUser> usersMap;

	public DBGuild(IGuild guild) {
		this.guild = guild;
		this.settingsMap = new HashMap<>();
		this.usersMap = new HashMap<>();

		this.load();
	}

	private void load() {
		JSONObject guildObj = Database.opt(guild.getStringID());
		if(guildObj == null) {
			return;
		}

		JSONObject settingsObj = guildObj.optJSONObject(SETTINGS_KEY);
		if(settingsObj != null) {
			for(Object settingKey : settingsObj.keySet()) {
				settingsMap.put(Setting.valueOf(settingKey.toString()), guildObj.get(settingKey.toString()));
			}
		}

		JSONObject usersObj = guildObj.optJSONObject(USERS_KEY);
		if(usersObj != null) {
			for(Object userKey : usersObj.keySet()) {
				long userID = Long.parseLong(userKey.toString());
				usersMap.put(userID, new DBUser(guild, userID));
			}
		}
	}

	public List<Long> getAllowedChannels() {
		if(settingsMap.containsKey(Setting.ALLOWED_CHANNELS)) {
			return JSONUtils.toList((JSONArray) settingsMap.get(Setting.ALLOWED_CHANNELS), Long.class);
		} else {
			return new ArrayList<>();
		}
	}

	public String getPrefix() {
		if(settingsMap.containsKey(Setting.PREFIX)) {
			return settingsMap.get(Setting.PREFIX).toString();
		} else {
			return Config.DEFAULT_PREFIX;
		}
	}

	public IGuild getGuild() {
		return guild;
	}

	public List<DBUser> getUsers() {
		return usersMap.values().stream().collect(Collectors.toList());
	}

	public void setSetting(Setting setting, Object value) {
		settingsMap.put(setting, value);
	}

	public void removeSetting(Setting setting) {
		settingsMap.remove(setting);
	}
}
