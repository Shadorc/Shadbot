package me.shadorc.shadbot.data.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.setting.SettingEnum;

public class DBGuild {

	@JsonProperty("id")
	private Long id;
	@JsonProperty("members")
	private CopyOnWriteArrayList<DBMember> members;
	@JsonProperty("settings")
	private ConcurrentHashMap<String, Object> settings;

	public DBGuild() {
		this.id = null;
		this.members = new CopyOnWriteArrayList<>();
		this.settings = new ConcurrentHashMap<>();
	}

	public DBGuild(Snowflake id) {
		this.id = id.asLong();
		this.members = new CopyOnWriteArrayList<>();
		this.settings = new ConcurrentHashMap<>();
	}

	@JsonIgnore
	public Snowflake getId() {
		return Snowflake.of(this.id);
	}

	@JsonIgnore
	public List<DBMember> getMembers() {
		return this.members;
	}

	@JsonIgnore
	public List<Snowflake> getAllowedTextChannels() {
		return this.getListSetting(SettingEnum.ALLOWED_TEXT_CHANNELS, Snowflake.class);
	}

	@JsonIgnore
	public List<Snowflake> getAllowedVoiceChannels() {
		return this.getListSetting(SettingEnum.ALLOWED_VOICE_CHANNELS, Snowflake.class);
	}

	@JsonIgnore
	public List<Snowflake> getAllowedRoles() {
		return this.getListSetting(SettingEnum.PERMISSIONS, Snowflake.class);
	}

	@JsonIgnore
	public List<Snowflake> getAutoRoles() {
		return this.getListSetting(SettingEnum.AUTO_ROLE, Snowflake.class);
	}

	@JsonIgnore
	public List<String> getBlacklistedCmd() {
		return this.getListSetting(SettingEnum.BLACKLIST, String.class);
	}

	@JsonIgnore
	public Integer getDefaultVol() {
		return Integer.parseInt(Objects.toString(
				this.settings.get(SettingEnum.DEFAULT_VOLUME.toString()),
				Integer.toString(Config.DEFAULT_VOLUME)));
	}

	/**
	 * @return A map containing message's ID as key and role's ID as value
	 */
	@SuppressWarnings("unchecked")
	@JsonIgnore
	public Map<Long, Long> getIamMessages() {
		return Optional.ofNullable((Map<Long, Long>) this.settings.get(SettingEnum.IAM_MESSAGES.toString()))
				.orElse(new HashMap<Long, Long>());
	}

	@JsonIgnore
	public Optional<String> getJoinMessage() {
		return Optional.ofNullable((String) this.settings.get(SettingEnum.JOIN_MESSAGE.toString()));
	}

	@JsonIgnore
	public Optional<String> getLeaveMessage() {
		return Optional.ofNullable((String) this.settings.get(SettingEnum.LEAVE_MESSAGE.toString()));
	}

	@JsonIgnore
	public Optional<Snowflake> getMessageChannelId() {
		return Optional.ofNullable((Snowflake) this.settings.get(SettingEnum.MESSAGE_CHANNEL_ID.toString()));
	}

	@JsonIgnore
	public String getPrefix() {
		return Objects.toString(
				this.settings.get(SettingEnum.PREFIX.toString()),
				Config.DEFAULT_PREFIX);
	}

	private <T> List<T> getListSetting(SettingEnum setting, Class<T> listClass) {
		return Optional.ofNullable((List<?>) this.settings.get(setting.toString()))
				.orElse(new ArrayList<>())
				.stream()
				.map(listClass::cast)
				.collect(Collectors.toList());
	}

	public void setSetting(SettingEnum setting, Object value) {
		this.settings.put(setting.toString(), value);
	}

	public void removeSetting(SettingEnum setting) {
		this.settings.remove(setting.toString());
	}

	public void addMember(DBMember dbMember) {
		this.members.add(dbMember);
	}

	@Override
	public String toString() {
		return String.format("DBGuild [id=%s, members=%s, settings=%s]", this.id, this.members, this.settings);
	}

}
