package me.shadorc.shadbot.core.setting;

import java.util.List;
import java.util.Optional;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.StringUtils;
import reactor.core.publisher.Mono;

public abstract class AbstractSetting {

	private final String description;
	private final SettingEnum setting;

	public AbstractSetting() {
		Setting settingAnnotation = this.getClass().getAnnotation(Setting.class);
		this.description = settingAnnotation.description();
		this.setting = settingAnnotation.setting();
	}

	public abstract Mono<Void> execute(Context context);

	public abstract Mono<EmbedCreateSpec> getHelp(Context context);

	public String getDescription() {
		return description;
	}

	public SettingEnum getSetting() {
		return setting;
	}

	public String getName() {
		return setting.toString();
	}

	public String getCommandName() {
		return String.format("setting %s", this.getName());
	}

	public Optional<String> getSettingArg(Context context) {
		Optional<String> arg = context.getArg();
		if(!arg.isPresent()) {
			return Optional.empty();
		}

		List<String> args = StringUtils.split(arg.get());

		if(args.size() == 1) {
			return Optional.empty();
		}

		return Optional.of(args.get(1));
	}

	public String requireArg(Context context) {
		return this.getSettingArg(context).orElseThrow(() -> new MissingArgumentException());
	}

}
