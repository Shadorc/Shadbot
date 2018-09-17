package me.shadorc.shadbot.data.database;

import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.utils.NumberUtils;

public class DBMember {

	private Long guildId;
	@JsonProperty("id")
	private Long id;
	@JsonProperty("coins")
	private AtomicInteger coins;

	public DBMember() {
		// Default constructor
	}

	public DBMember(Snowflake guildId, Snowflake id) {
		this.guildId = guildId.asLong();
		this.id = id.asLong();
		this.coins = new AtomicInteger(0);
	}

	@JsonIgnore
	public Snowflake getGuildId() {
		return Snowflake.of(this.guildId);
	}

	@JsonIgnore
	public Snowflake getId() {
		return Snowflake.of(this.id);
	}

	@JsonIgnore
	public int getCoins() {
		return this.coins.get();
	}

	public void addCoins(int gains) {
		this.coins.set(NumberUtils.between(this.getCoins() + gains, 0, Config.MAX_COINS));
	}

	public void resetCoins() {
		this.coins.set(0);
	}

	@Override
	public String toString() {
		return String.format("DBMember [guildId=%s, id=%s, coins=%s]", this.guildId, this.id, this.coins);
	}

}
