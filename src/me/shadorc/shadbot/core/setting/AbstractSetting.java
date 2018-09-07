package me.shadorc.shadbot.core.setting;

import java.util.List;
import java.util.Optional;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.utils.StringUtils;
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

	public abstract EmbedCreateSpec getHelp(Context context);

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

	public Optional<String> getSettingArg(Context context) {
		final Optional<String> arg = context.getArg();
		if(!arg.isPresent()) {
			return Optional.empty();
		}

		final List<String> args = StringUtils.split(arg.get(), 2);
		return args.size() == 1 ? Optional.empty() : Optional.of(args.get(1));
	}

}
