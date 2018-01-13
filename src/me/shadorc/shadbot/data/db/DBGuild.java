package me.shadorc.shadbot.data.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.command.admin.setting.core.SettingEnum;
import me.shadorc.shadbot.data.stats.Stats.DatabaseEnum;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.utils.Utils;
import sx.blah.discord.handle.obj.IGuild;

public class DBGuild {

	public static final String USERS_KEY = "users";
	private static final String SETTINGS_KEY = "settings";

	private final IGuild guild;
	private final Map<SettingEnum, Object> settingsMap;
	private final Map<Long, DBUser> usersMap;

	public DBGuild(IGuild guild) {
		this.guild = guild;
		this.settingsMap = new HashMap<>();
		this.usersMap = new HashMap<>();

		this.load();
	}

	private void load() {
		StatsManager.increment(DatabaseEnum.GUILD_LOADED);

		JSONObject guildObj = Database.opt(guild.getStringID());
		if(guildObj == null) {
			return;
		}

		JSONObject settingsObj = guildObj.optJSONObject(SETTINGS_KEY);
		if(settingsObj != null) {
			for(String settingKey : settingsObj.keySet()) {
				settingsMap.put(SettingEnum.valueOf(settingKey.toUpperCase()), settingsObj.get(settingKey));
			}
		}

		JSONObject usersObj = guildObj.optJSONObject(USERS_KEY);
		if(usersObj != null) {
			for(String userKey : usersObj.keySet()) {
				long userID = Long.parseLong(userKey);
				usersMap.put(userID, new DBUser(guild, userID));
			}
		}
	}

	public List<Long> getAllowedChannels() {
		if(settingsMap.containsKey(SettingEnum.ALLOWED_CHANNELS)) {
			return Utils.toList((JSONArray) settingsMap.get(SettingEnum.ALLOWED_CHANNELS), Long.class);
		} else {
			return new ArrayList<>();
		}
	}

	public List<String> getBlacklistedCmd() {
		if(settingsMap.containsKey(SettingEnum.BLACKLIST)) {
			return Utils.toList((JSONArray) settingsMap.get(SettingEnum.BLACKLIST), String.class);
		} else {
			return new ArrayList<>();
		}
	}

	public String getPrefix() {
		if(settingsMap.containsKey(SettingEnum.PREFIX)) {
			return settingsMap.get(SettingEnum.PREFIX).toString();
		} else {
			return Config.DEFAULT_PREFIX;
		}
	}

	public int getDefaultVol() {
		if(settingsMap.containsKey(SettingEnum.VOLUME)) {
			return Integer.parseInt(settingsMap.get(SettingEnum.VOLUME).toString());
		} else {
			return Config.DEFAULT_VOLUME;
		}
	}

	public Long getMessageChannelID() {
		return (Long) settingsMap.getOrDefault(SettingEnum.MESSAGE_CHANNEL_ID, null);
	}

	public String getJoinMessage() {
		return (String) settingsMap.get(SettingEnum.JOIN_MESSAGE);
	}

	public String getLeaveMessage() {
		return (String) settingsMap.get(SettingEnum.LEAVE_MESSAGE);
	}

	public IGuild getGuild() {
		return guild;
	}

	public List<DBUser> getUsers() {
		return usersMap.values().stream().collect(Collectors.toList());
	}

	public void setSetting(SettingEnum setting, Object value) {
		settingsMap.put(setting, value);
		Database.save(this);
	}

	public void removeSetting(SettingEnum setting) {
		settingsMap.remove(setting);
		Database.save(this);
	}

	public JSONObject toJSON() {
		JSONObject guildObj = new JSONObject();

		JSONObject settingsObj = new JSONObject();
		for(SettingEnum setting : settingsMap.keySet()) {
			settingsObj.put(setting.toString(), settingsMap.get(setting));
		}
		guildObj.put(SETTINGS_KEY, settingsObj);

		JSONObject usersObj = new JSONObject();
		for(Long userID : usersMap.keySet()) {
			usersObj.put(userID.toString(), usersMap.get(userID).toJSON());
		}
		guildObj.put(USERS_KEY, usersObj);

		return guildObj;
	}

}
