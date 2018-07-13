package me.shadorc.shadbot.data.db;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.json.JSONArray;

import com.fasterxml.jackson.annotation.JsonProperty;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.command.admin.setting.core.SettingEnum;
import me.shadorc.shadbot.utils.Utils;

public class DBGuild {

	@JsonProperty("id")
	private Snowflake id;
	@JsonProperty("members")
	private List<DBMember> members;
	@JsonProperty("settings")
	private Map<String, Object> settings;

	public Snowflake getId() {
		return id;
	}

	public List<DBMember> getMembers() {
		return members;
	}

	public List<Snowflake> getAllowedChannels() {
		return this.getSetting(SettingEnum.ALLOWED_CHANNELS, Long.class)
				.stream()
				.map(Snowflake::of)
				.collect(Collectors.toList());
	}

	public List<String> getBlacklistedCmd() {
		return this.getSetting(SettingEnum.BLACKLIST, String.class);
	}

	public List<Snowflake> getAutoRoles() {
		return this.getSetting(SettingEnum.AUTO_ROLE, Long.class)
				.stream()
				.map(Snowflake::of)
				.collect(Collectors.toList());
	}

	public List<Snowflake> getAllowedRoles() {
		return this.getSetting(SettingEnum.PERMISSIONS, Long.class)
				.stream()
				.map(Snowflake::of)
				.collect(Collectors.toList());
	}

	public String getPrefix() {
		return Objects.toString(
				settings.get(SettingEnum.PREFIX.toString()),
				Config.DEFAULT_PREFIX);
	}

	public Integer getDefaultVol() {
		return Integer.parseInt(Objects.toString(
				settings.get(SettingEnum.DEFAULT_VOLUME.toString()),
				Integer.toString(Config.DEFAULT_VOLUME)));
	}

	public Optional<Snowflake> getMessageChannelId() {
		return Optional.ofNullable((Long) settings.get(SettingEnum.MESSAGE_CHANNEL_ID.toString()))
				.map(Snowflake::of);
	}

	public Optional<String> getJoinMessage() {
		return Optional.ofNullable((String) settings.get(SettingEnum.JOIN_MESSAGE.toString()));
	}

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
