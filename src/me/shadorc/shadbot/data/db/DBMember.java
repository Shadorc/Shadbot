package me.shadorc.shadbot.data.db;

import org.json.JSONObject;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.data.stats.DatabaseStatsManager;
import me.shadorc.shadbot.data.stats.DatabaseStatsManager.DatabaseEnum;
import me.shadorc.shadbot.utils.NumberUtils;

public class DBMember {

	private static final String COINS_KEY = "coins";

	private final Snowflake id;
	private final Snowflake guildId;

	private int coins;

	public DBMember(Snowflake guildId, Snowflake memberId) {
		this.id = memberId;
		this.guildId = guildId;
		this.load();
	}

	private void load() {
		DatabaseStatsManager.log(DatabaseEnum.USER_LOADED);

		JSONObject guildObj = Database.opt(guildId.asString());
		if(guildObj == null) {
			return;
		}

		JSONObject membersObj = guildObj.optJSONObject(DBGuild.USERS_KEY);
		if(membersObj == null) {
			return;
		}

		JSONObject memberObj = membersObj.optJSONObject(id.asString());
		if(memberObj == null) {
			return;
		}

		this.coins = memberObj.optInt(COINS_KEY);
	}

	public Snowflake getGuildId() {
		return guildId;
	}

	public Snowflake getId() {
		return id;
	}

	public int getCoins() {
		return coins;
	}

	public void addCoins(int gains) {
		this.coins = NumberUtils.between(this.getCoins() + gains, 0, Config.MAX_COINS);
		Database.save(this);
	}

	public void resetCoins() {
		this.coins = 0;
		Database.save(this);
	}

	public JSONObject toJSON() {
		JSONObject memberObj = new JSONObject();
		if(coins != 0) {
			memberObj.put(COINS_KEY, coins);
		}
		return memberObj;
	}
}
