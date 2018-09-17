package me.shadorc.shadbot.data.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.setting.SettingEnum;

public class DBGuild {

	// TODO: Check concurrency everywhere

	@JsonProperty("id")
	private Long id;
	@JsonProperty("members")
	private List<DBMember> members;
	@JsonProperty("settings")
	private Map<String, Object> settings;

	public DBGuild() {
		// Default constructor
	}

	public DBGuild(Snowflake id) {
		this.id = id.asLong();
		this.members = new ArrayList<>();
		this.settings = new HashMap<>();
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
		return this.getSetting(SettingEnum.ALLOWED_TEXT_CHANNELS, Snowflake.class);
	}

	@JsonIgnore
	public List<Snowflake> getAllowedVoiceChannels() {
		return this.getSetting(SettingEnum.ALLOWED_VOICE_CHANNELS, Snowflake.class);
	}

	@JsonIgnore
	public List<String> getBlacklistedCmd() {
		return this.getSetting(SettingEnum.BLACKLIST, String.class);
	}

	@JsonIgnore
	public List<Snowflake> getAutoRoles() {
		return this.getSetting(SettingEnum.AUTO_ROLE, Snowflake.class);
	}

	@JsonIgnore
	public List<Snowflake> getAllowedRoles() {
		return this.getSetting(SettingEnum.PERMISSIONS, Snowflake.class);
	}

	@JsonIgnore
	public String getPrefix() {
		return Objects.toString(
				this.settings.get(SettingEnum.PREFIX.toString()),
				Config.DEFAULT_PREFIX);
	}

	@JsonIgnore
	public Integer getDefaultVol() {
		return Integer.parseInt(Objects.toString(
				this.settings.get(SettingEnum.DEFAULT_VOLUME.toString()),
				Integer.toString(Config.DEFAULT_VOLUME)));
	}

	@JsonIgnore
	public Optional<Snowflake> getMessageChannelId() {
		return Optional.ofNullable((Snowflake) this.settings.get(SettingEnum.MESSAGE_CHANNEL_ID.toString()));
	}

	@JsonIgnore
	public Optional<String> getJoinMessage() {
		return Optional.ofNullable((String) this.settings.get(SettingEnum.JOIN_MESSAGE.toString()));
	}

	@JsonIgnore
	public Optional<String> getLeaveMessage() {
		return Optional.ofNullable((String) this.settings.get(SettingEnum.LEAVE_MESSAGE.toString()));
	}

	/**
	 * @return A map containing message's ID as key and role's ID as value
	 */
	@SuppressWarnings("unchecked")
	@JsonIgnore
	public Map<Snowflake, Snowflake> getIamMessages() {
		return Optional.ofNullable((Map<Snowflake, Snowflake>) this.settings.get(SettingEnum.IAM_MESSAGES.toString()))
				.orElse(Collections.emptyMap());
	}

	private <T> List<T> getSetting(SettingEnum setting, Class<T> listClass) {
		return Optional.ofNullable((List<?>) this.settings.get(setting.toString()))
				.orElse(Collections.emptyList())
				.stream()
				.map(listClass::cast)
				.collect(Collectors.toList());
	}

	public void setSetting(SettingEnum setting, Object value) {
		this.settings.put(setting.toString(), value);
	}

	public void addMember(DBMember dbMember) {
		this.members.add(dbMember);
	}

	public void removeSetting(SettingEnum setting) {
		this.settings.remove(setting.toString());
	}

	@Override
	public String toString() {
		return String.format("DBGuild [id=%s, members=%s, settings=%s]", this.id, this.members, this.settings);
	}

}
