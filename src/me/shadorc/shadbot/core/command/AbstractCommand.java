package me.shadorc.shadbot.core.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.ratelimiter.RateLimiter;
import sx.blah.discord.api.internal.json.objects.EmbedObject;

public abstract class AbstractCommand {

	private final List<String> names;
	private final String alias;
	private final CommandCategory category;
	private final CommandPermission permission;
	private final RateLimiter rateLimiter;

	public AbstractCommand() {
		Command cmdAnnotation = this.getClass().getAnnotation(Command.class);
		this.names = new ArrayList<>(Arrays.asList(cmdAnnotation.names()));
		this.alias = cmdAnnotation.alias();
		this.category = cmdAnnotation.category();
		this.permission = cmdAnnotation.permission();

		RateLimited limiterAnnotation = this.getClass().getAnnotation(RateLimited.class);
		if(limiterAnnotation == null) {
			this.rateLimiter = null;
		} else {
			this.rateLimiter = new RateLimiter(limiterAnnotation.max(), limiterAnnotation.cooldown(), limiterAnnotation.unit());
		}
	}

	public abstract void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException;

	public abstract EmbedObject getHelp(String prefix);

	public List<String> getNames() {
		return names;
	}

	public String getName() {
		return this.getNames().get(0);
	}

	public String getAlias() {
		return alias;
	}

	public CommandCategory getCategory() {
		return category;
	}

	public CommandPermission getPermission() {
		return permission;
	}

	public RateLimiter getRateLimiter() {
		return rateLimiter;
	}
}
