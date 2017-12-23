package me.shadorc.shadbot;

import java.io.IOException;
import java.util.Properties;

import me.shadorc.shadbot.data.Database;
import me.shadorc.shadbot.utils.LogUtils;
import sx.blah.discord.api.IDiscordClient;

public class Shadbot {

	public static String version;

	private static Database dbManagaer;

	public static void main(String[] args) {
		dbManagaer = new Database();

		try {
			Properties properties = new Properties();
			properties.load(Shadbot.class.getClassLoader().getResourceAsStream("project.properties"));
			version = properties.getProperty("version");
		} catch (IOException err) {
			LogUtils.error("An error occurred while getting version.", err);
		}
	}

	public static IDiscordClient getClient() {
		return null;
	}

	public static Database getDBManager() {
		return dbManagaer;
	}

}
