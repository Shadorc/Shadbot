package me.shadorc.shadbot.data.db;

import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.utils.NumberUtils;

public class DBMember {

	private final Long guildId;
	@JsonProperty("id")
	private final Long id;
	@JsonProperty("coins")
	private final AtomicInteger coins;
	
	public DBMember() {
		this(Snowflake.of(0), Snowflake.of(0));
	}
	
	public DBMember(Snowflake guildId, Snowflake id) {
		this.guildId = guildId.asLong();
		this.id = id.asLong();
		this.coins = new AtomicInteger(0);
	}

	@JsonIgnore
	public Snowflake getGuildId() {
		return Snowflake.of(guildId);
	}

	@JsonIgnore
	public Snowflake getId() {
		return Snowflake.of(id);
	}

	@JsonIgnore
	public int getCoins() {
		return coins.get();
	}

	public void addCoins(int gains) {
		coins.set(NumberUtils.between(this.getCoins() + gains, 0, Config.MAX_COINS));
	}

	public void resetCoins() {
		coins.set(0);
	}

}
