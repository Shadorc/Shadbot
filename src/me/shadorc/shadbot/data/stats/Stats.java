package me.shadorc.shadbot.data.stats;

import me.shadorc.shadbot.data.annotation.StatsEnum;

public class Stats {

	@StatsEnum(name = "command", isSubdivided = true)
	public enum CommandEnum {
		COMMAND_USED,
		COMMAND_LIMITED,
		COMMAND_HELPED,
		COMMAND_MISSING_ARG,
		COMMAND_ILLEGAL_ARG
	}

	@StatsEnum(name = "money", isSubdivided = true)
	public enum MoneyEnum {
		MONEY_GAINED,
		MONEY_LOST
	}

	@StatsEnum(name = "various")
	public enum VariousEnum {
		MUSICS_LOADED,
		COMMANDS_EXECUTED,
		MESSAGES_RECEIVED,
		PRIVATE_MESSAGES_RECEIVED,
		MESSAGES_SENT,
		EMBEDS_SENT
	}

	@StatsEnum(name = "resources")
	public enum DatabaseEnum {
		GUILD_LOADED,
		USER_LOADED,
		GUILD_SAVED,
		USER_SAVED
	}
}
