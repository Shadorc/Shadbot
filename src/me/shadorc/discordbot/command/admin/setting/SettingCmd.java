package me.shadorc.discordbot.command.admin.setting;

import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.command.MissingArgumentException;

public interface SettingCmd {

	void execute(Context context, String arg) throws MissingArgumentException;

	void showHelp(Context context);

	String getDescription();
}