package me.shadorc.shadbot.data.db;

import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonProperty;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.utils.NumberUtils;

public class DBMember {

	private Snowflake guildId;
	@JsonProperty("id")
	private Snowflake id;
	@JsonProperty("coins")
	private AtomicInteger coins;

	public Snowflake getGuildId() {
		return guildId;
	}

	public Snowflake getId() {
		return id;
	}

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
