package me.shadorc.shadbot.data.db;

import java.util.Collections;
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
import reactor.core.publisher.Mono;

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
		return Mono.just(settingsMap.get(SettingEnum.ALLOWED_CHANNELS))
				.defaultIfEmpty(Collections.emptyList())
				.flatMapIterable(array -> Utils.toList((JSONArray) array, Long.class))
				.map(Snowflake::of)
				.collectList()
				.block();
	}

	public List<String> getBlacklistedCmd() {
		return Mono.just(settingsMap.get(SettingEnum.BLACKLIST))
				.defaultIfEmpty(Collections.emptyList())
				.map(array -> Utils.toList((JSONArray) array, String.class))
				.block();
	}

	public List<Snowflake> getAutoRoles() {
		return Mono.just(settingsMap.get(SettingEnum.AUTO_ROLE))
				.defaultIfEmpty(Collections.emptyList())
				.flatMapIterable(array -> Utils.toList((JSONArray) array, Long.class))
				.map(Snowflake::of)
				.collectList()
				.block();
	}

	public List<Snowflake> getAllowedRoles() {
		return Mono.just(settingsMap.get(SettingEnum.PERMISSIONS))
				.defaultIfEmpty(Collections.emptyList())
				.flatMapIterable(array -> Utils.toList((JSONArray) array, Long.class))
				.map(Snowflake::of)
				.collectList()
				.block();
	}

	public String getPrefix() {
		return Mono.just(settingsMap.get(SettingEnum.PREFIX))
				.defaultIfEmpty(Config.DEFAULT_PREFIX)
				.map(Object::toString)
				.block();
	}

	public Integer getDefaultVol() {
		return Mono.just(settingsMap.get(SettingEnum.DEFAULT_VOLUME))
				.defaultIfEmpty(Config.DEFAULT_VOLUME)
				.map(Object::toString)
				.map(Integer::parseInt)
				.block();
	}

	public Optional<Snowflake> getMessageChannelId() {
		return Optional.ofNullable(
				Snowflake.of((long) settingsMap.get(SettingEnum.MESSAGE_CHANNEL_ID)));
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
