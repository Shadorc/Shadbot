package me.shadorc.shadbot.command.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import discord4j.common.json.EmbedFieldEntity;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.setting.AbstractSetting;
import me.shadorc.shadbot.core.setting.Setting;
import me.shadorc.shadbot.core.setting.SettingEnum;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Mono;

@Command(category = CommandCategory.ADMIN, permission = CommandPermission.ADMIN, names = { "setting", "settings" })
public class SettingsCmd extends AbstractCommand {

	private static final Map<SettingEnum, AbstractSetting> SETTINGS_MAP = new HashMap<>();

	static {
		Reflections reflections = new Reflections(SettingsCmd.class.getPackage().getName(), new SubTypesScanner(), new TypeAnnotationsScanner());
		for(Class<?> settingClass : reflections.getTypesAnnotatedWith(Setting.class)) {
			final String settingName = settingClass.getSimpleName();
			if(!AbstractSetting.class.isAssignableFrom(settingClass)) {
				LogUtils.error(String.format("An error occurred while generating setting, %s cannot be cast to %s.",
						settingName, AbstractSetting.class.getSimpleName()));
				continue;
			}

			try {
				AbstractSetting settingCmd = (AbstractSetting) settingClass.getConstructor().newInstance();
				if(SETTINGS_MAP.putIfAbsent(settingCmd.getSetting(), settingCmd) != null) {
					LogUtils.error(String.format("Command name collision between %s and %s",
							settingName, SETTINGS_MAP.get(settingCmd.getSetting()).getClass().getSimpleName()));
					continue;
				}
			} catch (Exception err) {
				LogUtils.error(err, String.format("An error occurred while initializing setting %s.", settingName));
			}
		}
	}

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(2);

		final SettingEnum settingEnum = Utils.getEnum(SettingEnum.class, args.get(0));
		final AbstractSetting setting = SETTINGS_MAP.get(settingEnum);

		if(setting == null) {
			throw new CommandException(String.format("Setting `%s` does not exist. Use `%shelp %s` to see all available settings.",
					args.get(0), context.getPrefix(), this.getName()));
		}

		final String arg = args.size() == 2 ? args.get(1) : null;
		if("help".equals(arg)) {
			return this.getHelp(context, setting)
					.flatMap(embed -> BotUtils.sendMessage(embed, context.getChannel()))
					.then();
		}

		try {
			return setting.execute(context);
		} catch (MissingArgumentException e) {
			return this.getHelp(context, setting)
					.flatMap(embed -> BotUtils.sendMessage(TextUtils.MISSING_ARG, embed, context.getChannel()))
					.then();
		}
	}

	private Mono<EmbedCreateSpec> getHelp(Context context, AbstractSetting setting) {
		return context.getAvatarUrl()
				.map(avatarUrl -> setting.getHelp(context)
						.setAuthor(String.format("Help for setting: %s", setting.getName()), null, avatarUrl)
						.setDescription(String.format("**%s**", setting.getDescription())));
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		HelpBuilder embed = new HelpBuilder(this, context)
				.setThumbnail("http://www.emoji.co.uk/files/emoji-one/objects-emoji-one/1898-gear.png")
				.setDescription("Change Shadbot's settings for this server.")
				.addArg("name", false)
				.addArg("args", false)
				.addField("Additional Help", String.format("`%s%s <name> help`",
						context.getPrefix(), this.getName()), false);

		SETTINGS_MAP.values().stream()
				.map(setting -> new EmbedFieldEntity(String.format("Name: %s", setting.getName()), setting.getDescription(), false))
				.forEach(field -> embed.addField(field.getName(), field.getValue(), field.isInline()));

		return embed.build();
	}

}
