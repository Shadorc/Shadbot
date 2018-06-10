package me.shadorc.shadbot.command.admin.setting.core;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;

public abstract class AbstractSetting {

	private final String description;
	private final SettingEnum setting;

	public AbstractSetting() {
		Setting settingAnnot = this.getClass().getAnnotation(Setting.class);
		this.description = settingAnnot.description();
		this.setting = settingAnnot.setting();
	}

	public abstract void execute(Context context, String arg);

	public abstract EmbedCreateSpec getHelp(String prefix);

	public String getDescription() {
		return description;
	}

	public SettingEnum getSetting() {
		return setting;
	}

	public String getName() {
		return setting.toString();
	}

	public String getCmdName() {
		return String.format("setting %s", this.getName());
	}

}
