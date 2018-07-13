package me.shadorc.shadbot.data.db;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.data.DataManager;
import me.shadorc.shadbot.data.annotation.DataInit;
import me.shadorc.shadbot.data.annotation.DataSave;
import me.shadorc.shadbot.utils.Utils;

public class DatabaseManager {

	private static final String FILE_NAME = "database.json";
	private static final File FILE = new File(DataManager.SAVE_DIR, FILE_NAME);

	private static Database database;

	@DataInit
	public static void init() throws IOException {
		database = FILE.exists() ? Utils.MAPPER.readValue(FILE, Database.class) : new Database();
	}

	@DataSave(filePath = FILE_NAME, initialDelay = 15, period = 15, unit = TimeUnit.MINUTES)
	public static void save() throws IOException {
		try (FileWriter writer = new FileWriter(FILE)) {
			writer.write(Utils.MAPPER.writeValueAsString(database));
		}
	}

	public static DBGuild getDBGuild(Snowflake guildId) {
		Optional<DBGuild> dbGuildOpt = database.getGuilds().stream()
				.filter(guild -> guild.getId().equals(guildId))
				.findFirst();

		if(dbGuildOpt.isPresent()) {
			return dbGuildOpt.get();
		}

		final DBGuild dbGuild = new DBGuild(guildId);
		database.addGuild(dbGuild);
		return dbGuild;
	}

	public static DBMember getDBMember(Snowflake guildId, Snowflake memberId) {
		Optional<DBMember> dbMemberOpt = DatabaseManager.getDBGuild(guildId)
				.getMembers()
				.stream()
				.filter(member -> member.getId().equals(memberId))
				.findFirst();

		if(dbMemberOpt.isPresent()) {
			return dbMemberOpt.get();
		}

		final DBMember dbMember = new DBMember(guildId, memberId);
		DatabaseManager.getDBGuild(guildId).addMember(dbMember);
		return dbMember;
	}

}
