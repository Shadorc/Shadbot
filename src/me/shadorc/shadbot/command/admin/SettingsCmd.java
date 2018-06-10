package me.shadorc.shadbot.command.admin;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import me.shadorc.shadbot.command.admin.setting.core.AbstractSetting;
import me.shadorc.shadbot.command.admin.setting.core.Setting;
import me.shadorc.shadbot.command.admin.setting.core.SettingEnum;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.exception.IllegalCmdArgumentException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.embed.log.LogUtils;

@Command(category = CommandCategory.ADMIN, permission = CommandPermission.ADMIN, names = { "setting", "settings" })
public class SettingsCmd extends AbstractCommand {

	private static final Map<SettingEnum, AbstractSetting> SETTINGS_MAP = new HashMap<>();

	static {
		Reflections reflections = new Reflections(SettingsCmd.class.getPackage().getName(), new SubTypesScanner(), new TypeAnnotationsScanner());
		for(Class<?> settingClass : reflections.getTypesAnnotatedWith(Setting.class)) {
			String settindName = settingClass.getSimpleName();
			if(!AbstractSetting.class.isAssignableFrom(settingClass)) {
				LogUtils.error(String.format("An error occurred while generating setting, %s cannot be cast to AbstractSetting.",
						settindName));
				continue;
			}

			try {
				AbstractSetting settingCmd = (AbstractSetting) settingClass.getConstructor().newInstance();
				if(SETTINGS_MAP.putIfAbsent(settingCmd.getSetting(), settingCmd) != null) {
					LogUtils.error(String.format("Command name collision between %s and %s",
							settindName, SETTINGS_MAP.get(settingCmd.getSetting()).getClass().getSimpleName()));
					continue;
				}
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException err) {
				LogUtils.error(err, String.format("An error occurred while initializing setting %s.", settindName));
			}
		}
	}

	@Override
	public void execute(Context context) {
		List<String> args = context.requireArgs(2);

		SettingEnum settingEnum = Utils.getValueOrNull(SettingEnum.class, args.get(0));
		if(settingEnum == null || !SETTINGS_MAP.containsKey(settingEnum)) {
			throw new IllegalCmdArgumentException(String.format("Setting `%s` does not exist. Use `%shelp %s` to see all available settings.",
					args.get(0), context.getPrefix(), this.getName()));
		}

		AbstractSetting setting = SETTINGS_MAP.get(settingEnum);

		String arg = args.size() == 2 ? args.get(1) : null;
		if("help".equals(arg)) {
			BotUtils.sendMessage(this.getHelp(context.getPrefix(), setting), context.getChannel());
			return;
		}

		try {
			setting.execute(context, arg);
		} catch (MissingArgumentException e) {
			BotUtils.sendMessage(new MessageCreateSpec()
					.setContent(TextUtils.MISSING_ARG)
					.setEmbed(this.getHelp(context.getPrefix(), setting)), context.getChannel());
		}
	}

	private EmbedCreateSpec getHelp(String prefix, AbstractSetting setting) {
		return setting.getHelp(prefix)
				.setAuthor(String.format("Help for setting: %s", setting.getName()), null, null)
				.setDescription(String.format("**%s**", setting.getDescription()));
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		HelpBuilder embed = new HelpBuilder(this, prefix)
				.setThumbnail("http://www.emoji.co.uk/files/emoji-one/objects-emoji-one/1898-gear.png")
				.setDescription("Change Shadbot's settings for this server.")
				.addArg("name", false)
				.addArg("args", false)
				.addField("Additional Help", String.format("`%s%s <name> help`", prefix, this.getName()), false);

		SETTINGS_MAP.values().stream()
				.forEach(setting -> embed.addField(String.format("Name: %s", setting.getName()), setting.getDescription(), false));

		return embed.build();
	}

}
