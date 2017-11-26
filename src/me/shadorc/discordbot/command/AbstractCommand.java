package me.shadorc.discordbot.command;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.shadorc.discordbot.stats.StatsEnum;
import me.shadorc.discordbot.stats.StatsManager;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;

public abstract class AbstractCommand {

	private final RateLimiter rateLimiter;
	private final CommandCategory category;
	private final Role role;
	private final List<String> names;

	private String alias;

	private AbstractCommand(CommandCategory category, Role role, RateLimiter rateLimiter, String name, String... names) {
		this.rateLimiter = rateLimiter;
		this.category = category;
		this.role = role;
		this.names = new ArrayList<>(Arrays.asList(names));
		this.names.add(0, name);
	}

	public AbstractCommand(CommandCategory category, Role role, int timeout, String name, String... names) {
		this(category, role, new RateLimiter(timeout, ChronoUnit.SECONDS), name, names);
	}

	public AbstractCommand(CommandCategory category, Role role, String name, String... names) {
		this(category, role, null, name, names);
	}

	public abstract void execute(Context context) throws MissingArgumentException;

	public abstract void showHelp(Context context);

	public final void checkSpamAndExecute(Context context) throws MissingArgumentException {
		if(rateLimiter != null && rateLimiter.isSpamming(context.getChannel(), context.getAuthor())) {
			StatsManager.increment(StatsEnum.LIMITED_COMMAND, context.getCommand());
			return;
		}
		StatsManager.increment(StatsEnum.COMMANDS_EXECUTED);
		this.execute(context);
	}

	public final CommandCategory getCategory() {
		return category;
	}

	public final Role getRole() {
		return role;
	}

	public final List<String> getNames() {
		return names;
	}

	public final String getFirstName() {
		return names.get(0);
	}

	public final String getAlias() {
		return alias;
	}

	public final void setAlias(String alias) {
		this.alias = alias;
		this.names.add(alias);
	}
}
