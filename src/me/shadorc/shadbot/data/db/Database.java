package me.shadorc.shadbot.data.db;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Database {

	@JsonProperty("guilds")
	private List<DBGuild> guilds;

	public List<DBGuild> getGuilds() {
		return guilds;
	}

	public void addGuild(DBGuild dbGuild) {
		guilds.add(dbGuild);
	}

}
