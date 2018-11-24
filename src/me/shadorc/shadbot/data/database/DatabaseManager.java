package me.shadorc.shadbot.data.database;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.databind.JavaType;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.data.Data;
import me.shadorc.shadbot.utils.Utils;

public class DatabaseManager extends Data {

	private final List<DBGuild> guilds;

	public DatabaseManager() throws IOException {
		super("database.json", Duration.ofMinutes(15), Duration.ofMinutes(15));

		final JavaType valueType = Utils.MAPPER.getTypeFactory().constructCollectionType(CopyOnWriteArrayList.class, DBGuild.class);
		this.guilds = this.getFile().exists() ? Utils.MAPPER.readValue(this.getFile(), valueType) : new CopyOnWriteArrayList<>();
	}

	public void write() throws IOException {
		try (FileWriter writer = new FileWriter(this.getFile())) {
			writer.write(Utils.MAPPER.writeValueAsString(guilds));
		}
	}

	public DBGuild getDBGuild(Snowflake guildId) {
		final Optional<DBGuild> dbGuildOpt = guilds.stream()
				.filter(guild -> guild.getId().equals(guildId))
				.findFirst();

		if(dbGuildOpt.isPresent()) {
			return dbGuildOpt.get();
		}

		final DBGuild dbGuild = new DBGuild(guildId);
		guilds.add(dbGuild);
		return dbGuild;
	}

	public DBMember getDBMember(Snowflake guildId, Snowflake memberId) {
		final Optional<DBMember> dbMemberOpt = this.getDBGuild(guildId)
				.getMembers()
				.stream()
				.filter(member -> member.getId().equals(memberId))
				.findFirst();

		if(dbMemberOpt.isPresent()) {
			return dbMemberOpt.get();
		}

		final DBMember dbMember = new DBMember(guildId, memberId);
		this.getDBGuild(guildId).addMember(dbMember);
		return dbMember;
	}

}
