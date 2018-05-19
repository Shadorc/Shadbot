package me.shadorc.shadbot.data.lotto;

import discord4j.core.object.util.Snowflake;

public class LottoPlayer {

	private final Snowflake guildId;
	private final Snowflake userId;
	private final int num;

	public LottoPlayer(Snowflake guildId, Snowflake userId, int num) {
		this.guildId = guildId;
		this.userId = userId;
		this.num = num;
	}

	public Snowflake getGuildId() {
		return guildId;
	}

	public Snowflake getUserId() {
		return userId;
	}

	public int getNum() {
		return num;
	}

}
