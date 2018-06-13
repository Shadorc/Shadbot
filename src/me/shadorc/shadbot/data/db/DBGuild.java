package me.shadorc.shadbot.data.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.command.admin.setting.core.SettingEnum;
import me.shadorc.shadbot.data.stats.DatabaseStatsManager;
import me.shadorc.shadbot.data.stats.DatabaseStatsManager.DatabaseEnum;
import me.shadorc.shadbot.utils.Utils;

public class DBGuild {

	public static final String USERS_KEY = "users";
	private static final String SETTINGS_KEY = "settings";

	private final Snowflake id;
	private final Map<SettingEnum, Object> settingsMap;
	private final Map<Snowflake, DBMember> usersMap;

	public DBGuild(Snowflake id) {
		this.id = id;
		this.settingsMap = new HashMap<>();
		this.usersMap = new HashMap<>();

		this.load();
	}

	private void load() {
		DatabaseStatsManager.log(DatabaseEnum.GUILD_LOADED);

		JSONObject guildObj = Database.opt(id.asString());
		if(guildObj == null) {
			return;
		}

		JSONObject settingsObj = guildObj.optJSONObject(SETTINGS_KEY);
		if(settingsObj != null) {
			for(String settingKey : settingsObj.keySet()) {
				settingsMap.put(SettingEnum.valueOf(settingKey.toUpperCase()), settingsObj.get(settingKey));
			}
		}
	}

	private void loadUsers() {
		JSONObject guildObj = Database.opt(id.asString());
		if(guildObj == null) {
			return;
		}

		JSONObject usersObj = guildObj.optJSONObject(USERS_KEY);
		if(usersObj != null) {
			for(String userKey : usersObj.keySet()) {
				Snowflake userId = Snowflake.of(userKey);
				usersMap.put(userId, new DBMember(id, userId));
			}
		}
	}

	public List<Snowflake> getAllowedChannels() {
		JSONArray array = (JSONArray) Optional.ofNullable(settingsMap.get(SettingEnum.ALLOWED_CHANNELS)).orElse(new JSONArray());
		return Utils.toList(array, Long.class)
				.stream()
				.map(Snowflake::of)
				.collect(Collectors.toList());
	}

	public List<String> getBlacklistedCmd() {
		JSONArray array = (JSONArray) Optional.ofNullable(settingsMap.get(SettingEnum.BLACKLIST)).orElse(new JSONArray());
		return Utils.toList(array, String.class);
	}

	public List<Snowflake> getAutoRoles() {
		JSONArray array = (JSONArray) Optional.ofNullable(settingsMap.get(SettingEnum.AUTO_ROLE)).orElse(new JSONArray());
		return Utils.toList(array, Long.class)
				.stream()
				.map(Snowflake::of)
				.collect(Collectors.toList());
	}

	public List<Snowflake> getAllowedRoles() {
		JSONArray array = (JSONArray) Optional.ofNullable(settingsMap.get(SettingEnum.PERMISSIONS)).orElse(new JSONArray());
		return Utils.toList(array, Long.class)
				.stream()
				.map(Snowflake::of)
				.collect(Collectors.toList());
	}

	public String getPrefix() {
		return Optional.ofNullable(settingsMap.get(SettingEnum.PREFIX)).orElse(Config.DEFAULT_PREFIX).toString();
	}

	public Integer getDefaultVol() {
		return Integer.parseInt(Optional.ofNullable(settingsMap.get(SettingEnum.DEFAULT_VOLUME))
				.orElse(Config.DEFAULT_VOLUME)
				.toString());
	}

	public Optional<Snowflake> getMessageChannelId() {
		return Optional.ofNullable((Long) settingsMap.get(SettingEnum.MESSAGE_CHANNEL_ID))
				.map(Snowflake::of);
	}

	public Optional<String> getJoinMessage() {
		return Optional.ofNullable(
				(String) settingsMap.get(SettingEnum.JOIN_MESSAGE));
	}

	public Optional<String> getLeaveMessage() {
		return Optional.ofNullable(
				(String) settingsMap.get(SettingEnum.LEAVE_MESSAGE));
	}

	public Snowflake getId() {
		return id;
	}

	public List<DBMember> getUsers() {
		this.loadUsers();
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

		this.loadUsers();

		JSONObject usersObj = new JSONObject();
		for(Snowflake userId : usersMap.keySet()) {
			usersObj.put(userId.asString(), usersMap.get(userId).toJSON());
		}
		guildObj.put(USERS_KEY, usersObj);

		return guildObj;
	}

}
