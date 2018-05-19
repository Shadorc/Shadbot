package me.shadorc.shadbot.data.stats;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.data.annotation.DataInit;
import me.shadorc.shadbot.data.annotation.DataSave;
import me.shadorc.shadbot.data.stats.annotation.StatsInit;
import me.shadorc.shadbot.data.stats.annotation.StatsJSON;
import me.shadorc.shadbot.utils.LogUtils;

public class StatsManager {

	private static final Map<String, Supplier<EmbedCreateSpec>> EMBED_MAP = new HashMap<>();

	private static final String FILE_NAME = "stats.json";
	private static final File FILE = new File(FILE_NAME);

	@DataInit
	public static void init() throws IOException {
		if(!FILE.exists()) {
			try (FileWriter writer = new FileWriter(FILE)) {
				writer.write(new JSONObject().toString(Config.JSON_INDENT_FACTOR));
			}
		}

		JSONObject statsObj;
		try (InputStream stream = FILE.toURI().toURL().openStream()) {
			statsObj = new JSONObject(new JSONTokener(stream));
		}

		Reflections reflections = new Reflections(StatsManager.class.getPackage().getName(), new MethodAnnotationsScanner());
		for(Method initMethod : reflections.getMethodsAnnotatedWith(StatsInit.class)) {
			StatsInit annotation = initMethod.getAnnotation(StatsInit.class);
			try {
				initMethod.invoke(null, statsObj.has(annotation.name()) ? statsObj.getJSONObject(annotation.name()) : new JSONObject());
			} catch (Exception err) {
				LogUtils.error(err, String.format("An error occurred while initializing statistics %s.",
						initMethod.getDeclaringClass().getSimpleName()));
			}
		}
	}

	public static void register(String name, Supplier<EmbedCreateSpec> supplier) {
		EMBED_MAP.put(name, supplier);
	}

	public static Map<String, Supplier<EmbedCreateSpec>> getStats() {
		return EMBED_MAP;
	}

	@DataSave(filePath = FILE_NAME, initialDelay = 10, period = 10, unit = TimeUnit.MINUTES)
	public static void save() throws JSONException, IOException {
		JSONObject mainObj = new JSONObject();

		Reflections reflections = new Reflections(StatsManager.class.getPackage().getName(), new MethodAnnotationsScanner());
		for(Method jsonMethod : reflections.getMethodsAnnotatedWith(StatsJSON.class)) {
			StatsJSON annotation = jsonMethod.getAnnotation(StatsJSON.class);
			try {
				mainObj.put(annotation.name(), jsonMethod.invoke(null));
			} catch (Exception err) {
				LogUtils.error(err, String.format("An error occurred while saving statistics %s.",
						jsonMethod.getDeclaringClass().getSimpleName()));
			}
		}

		try (FileWriter writer = new FileWriter(FILE)) {
			writer.write(mainObj.toString(Config.JSON_INDENT_FACTOR));
		}
	}

}