package me.shadorc.shadbot.core.setting;

import java.util.function.Consumer;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import reactor.core.publisher.Mono;

public abstract class AbstractSetting {

	private final String description;
	private final SettingEnum setting;

	public AbstractSetting() {
		final Setting settingAnnotation = this.getClass().getAnnotation(Setting.class);
		this.description = settingAnnotation.description();
		this.setting = settingAnnotation.setting();
	}

	public abstract Mono<Void> execute(Context context);

	public abstract Consumer<EmbedCreateSpec> getHelp(Context context);

	public String getDescription() {
		return this.description;
	}

	public SettingEnum getSetting() {
		return this.setting;
	}

	public String getName() {
		return this.setting.toString();
	}

	public String getCommandName() {
		return String.format("setting %s", this.getName());
	}

}
