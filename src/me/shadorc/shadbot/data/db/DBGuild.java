package me.shadorc.shadbot.data.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.json.JSONArray;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.setting.SettingEnum;
import me.shadorc.shadbot.utils.Utils;

public class DBGuild {

	@JsonProperty("id")
	private final Long id;
	@JsonProperty("members")
	private final List<DBMember> members;
	@JsonProperty("settings")
	private final Map<String, Object> settings;

	public DBGuild() {
		this(Snowflake.of(0));
	}

	public DBGuild(Snowflake id) {
		this.id = id.asLong();
		this.members = new ArrayList<>();
		this.settings = new HashMap<>();
	}

	@JsonIgnore
	public Snowflake getId() {
		return Snowflake.of(id);
	}

	@JsonIgnore
	public List<DBMember> getMembers() {
		return members;
	}

	@JsonIgnore
	public List<Snowflake> getAllowedChannels() {
		return this.getSetting(SettingEnum.ALLOWED_CHANNELS, Long.class)
				.stream()
				.map(Snowflake::of)
				.collect(Collectors.toList());
	}

	@JsonIgnore
	public List<String> getBlacklistedCmd() {
		return this.getSetting(SettingEnum.BLACKLIST, String.class);
	}

	@JsonIgnore
	public List<Snowflake> getAutoRoles() {
		return this.getSetting(SettingEnum.AUTO_ROLE, Long.class)
				.stream()
				.map(Snowflake::of)
				.collect(Collectors.toList());
	}

	@JsonIgnore
	public List<Snowflake> getAllowedRoles() {
		return this.getSetting(SettingEnum.PERMISSIONS, Long.class)
				.stream()
				.map(Snowflake::of)
				.collect(Collectors.toList());
	}

	@JsonIgnore
	public String getPrefix() {
		return Objects.toString(
				settings.get(SettingEnum.PREFIX.toString()),
				Config.DEFAULT_PREFIX);
	}

	@JsonIgnore
	public Integer getDefaultVol() {
		return Integer.parseInt(Objects.toString(
				settings.get(SettingEnum.DEFAULT_VOLUME.toString()),
				Integer.toString(Config.DEFAULT_VOLUME)));
	}

	@JsonIgnore
	public Optional<Snowflake> getMessageChannelId() {
		return Optional.ofNullable((Long) settings.get(SettingEnum.MESSAGE_CHANNEL_ID.toString()))
				.map(Snowflake::of);
	}

	@JsonIgnore
	public Optional<String> getJoinMessage() {
		return Optional.ofNullable((String) settings.get(SettingEnum.JOIN_MESSAGE.toString()));
	}

	@JsonIgnore
	public Optional<String> getLeaveMessage() {
		return Optional.ofNullable((String) settings.get(SettingEnum.LEAVE_MESSAGE.toString()));
	}

	private <T> List<T> getSetting(SettingEnum setting, Class<T> listClass) {
		final JSONArray array = (JSONArray) Optional.ofNullable(settings.get(setting.toString())).orElse(new JSONArray());
		return Utils.toList(array, listClass);
	}

	public void setSetting(SettingEnum setting, Object value) {
		settings.put(setting.toString(), value);
	}

	public void addMember(DBMember dbMember) {
		members.add(dbMember);
	}

	public void removeSetting(SettingEnum setting) {
		settings.remove(setting.toString());
	}

}
