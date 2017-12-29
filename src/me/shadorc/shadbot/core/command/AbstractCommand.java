package me.shadorc.shadbot.core.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.exception.MissingArgumentException;
import sx.blah.discord.api.internal.json.objects.EmbedObject;

public abstract class AbstractCommand {

	private final List<String> names;
	private final String alias;
	private final CommandCategory category;
	private final CommandPermission permission;
	private final boolean isHidden;

	public AbstractCommand() {
		Command annotation = this.getClass().getAnnotation(Command.class);
		this.names = new ArrayList<>(Arrays.asList(annotation.names()));
		this.alias = annotation.alias();
		this.category = annotation.category();
		this.permission = annotation.permission();
		this.isHidden = annotation.hidden();
	}

	public abstract void execute(Context context) throws MissingArgumentException, IllegalArgumentException;

	// TODO: Is Context necessary ? Prefix should be the only thing needed
	public abstract EmbedObject getHelp(Context context);

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

	public boolean isHidden() {
		return isHidden;
	}
}
