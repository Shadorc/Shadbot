package me.shadorc.shadbot.command.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

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
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

@Command(category = CommandCategory.ADMIN, permission = CommandPermission.ADMIN, names = { "setting", "settings" })
public class SettingsCmd extends AbstractCommand {

	private static final Map<SettingEnum, AbstractSetting> SETTINGS_MAP = new HashMap<>();

	static {
		Reflections reflections = new Reflections(SettingsCmd.class.getPackage().getName(), new SubTypesScanner(), new TypeAnnotationsScanner());
		for(Class<?> settingClass : reflections.getTypesAnnotatedWith(Setting.class)) {
			if(!AbstractSetting.class.isAssignableFrom(settingClass)) {
				LogUtils.errorf("An error occurred while generating setting, %s cannot be cast to AbstractSetting.",
						settingClass.getSimpleName());
				continue;
			}

			try {
				AbstractSetting settingCmd = (AbstractSetting) settingClass.newInstance();
				if(SETTINGS_MAP.putIfAbsent(settingCmd.getSetting(), settingCmd) != null) {
					LogUtils.errorf(String.format("Command name collision between %s and %s",
							settingClass.getSimpleName(), SETTINGS_MAP.get(settingCmd.getSetting()).getClass().getSimpleName()));
					continue;
				}
			} catch (InstantiationException | IllegalAccessException err) {
				LogUtils.errorf(err, "An error occurred while initializing setting %s.", settingClass.getDeclaringClass().getSimpleName());
			}
		}
	}

	@Override
	public void execute(Context context) throws MissingArgumentException, IllegalCmdArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		List<String> splitArgs = StringUtils.split(context.getArg(), 2);
		if(splitArgs.size() != 2) {
			throw new MissingArgumentException();
		}

		SettingEnum settingEnum = Utils.getValueOrNull(SettingEnum.class, splitArgs.get(0));
		if(settingEnum == null) {
			throw new IllegalCmdArgumentException(String.format("This setting does not exist. Use `%shelp %s` to see all available settings.",
					context.getPrefix(), this.getName()));
		}

		AbstractSetting setting = SETTINGS_MAP.get(settingEnum);

		String arg = splitArgs.get(1);
		if("help".equals(arg)) {
			this.showHelp(context, setting);
			return;
		}

		try {
			setting.execute(context, arg);
		} catch (MissingArgumentException e) {
			this.showHelp(context, setting);
		}
	}

	private void showHelp(Context context, AbstractSetting setting) {
		EmbedBuilder embed = setting.getHelp(context.getPrefix())
				.withAuthorName(String.format("Help for setting: %s", setting.getName()))
				.appendDescription(String.format("**%s**", setting.getDescription()));
		BotUtils.sendMessage(embed.build(), context.getChannel());
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		HelpBuilder embed = new HelpBuilder(this, prefix)
				.setThumbnail("http://www.emoji.co.uk/files/emoji-one/objects-emoji-one/1898-gear.png")
				.setDescription("Change Shadbot's settings for this server.")
				.addArg("name", false)
				.addArg("args", false)
				.appendField("Additional Help", String.format("`%s%s <name> help`", prefix, this.getName()), false);

		SETTINGS_MAP.values().stream()
				.forEach(setting -> embed.appendField(String.format("Name: %s", setting.getName()), setting.getDescription(), false));

		return embed.build();
	}

}
