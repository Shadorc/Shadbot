package me.shadorc.discordbot.command.admin.setting;

import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.command.Context;

public interface SettingCmd {

	void execute(Context context, String arg) throws MissingArgumentException;

	void showHelp(Context context);

	String getDescription();
}