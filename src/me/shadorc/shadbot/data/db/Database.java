package me.shadorc.shadbot.data.db;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Database {

	@JsonProperty("guilds")
	private final List<DBGuild> guilds;

	public Database() {
		this.guilds = new ArrayList<>();
	}

	public List<DBGuild> getGuilds() {
		return guilds;
	}

	public void addGuild(DBGuild dbGuild) {
		guilds.add(dbGuild);
	}

}
