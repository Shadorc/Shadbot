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
	private final long guildId;
	@JsonProperty("members")
	private final CopyOnWriteArrayList<DBMember> members;
	@JsonProperty("settings")
	private final ConcurrentHashMap<String, Object> settings;

	public DBGuild(Snowflake id) {
		this.guildId = id.asLong();
		this.members = new CopyOnWriteArrayList<>();
		this.settings = new ConcurrentHashMap<>();
	}

	public DBGuild() {
		this(Snowflake.of(0L));
	}

	@JsonIgnore
	public Snowflake getId() {
		return Snowflake.of(this.guildId);
	}

	@JsonIgnore
	public List<DBMember> getMembers() {
		return this.members;
	}

	@JsonIgnore
	public List<Snowflake> getAllowedTextChannels() {
		return this.getListSetting(SettingEnum.ALLOWED_TEXT_CHANNELS, Long.class)
				.stream()
				.map(Snowflake::of)
				.collect(Collectors.toList());
	}

	@JsonIgnore
	public List<Snowflake> getAllowedVoiceChannels() {
		return this.getListSetting(SettingEnum.ALLOWED_VOICE_CHANNELS, Long.class)
				.stream()
				.map(Snowflake::of)
				.collect(Collectors.toList());
	}

	@JsonIgnore
	public List<Snowflake> getAllowedRoles() {
		return this.getListSetting(SettingEnum.ALLOWED_ROLES, Long.class)
				.stream()
				.map(Snowflake::of)
				.collect(Collectors.toList());
	}

	@JsonIgnore
	public List<Snowflake> getAutoRoles() {
		return this.getListSetting(SettingEnum.AUTO_ROLES, Long.class)
				.stream()
				.map(Snowflake::of)
				.collect(Collectors.toList());
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
	public Map<String, Long> getIamMessages() {
		return (Map<String, Long>) Optional.ofNullable(this.settings.get(SettingEnum.IAM_MESSAGES.toString()))
				.orElse(new HashMap<>());
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
		return Optional.ofNullable((Long) this.settings.get(SettingEnum.MESSAGE_CHANNEL_ID.toString()))
				.map(Snowflake::of);
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

}
