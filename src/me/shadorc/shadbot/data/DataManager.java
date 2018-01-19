package me.shadorc.shadbot.data;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;

import me.shadorc.shadbot.data.annotation.DataInit;
import me.shadorc.shadbot.data.annotation.DataSave;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.executor.ShadbotScheduledExecutor;

public class DataManager {

	public static final File SAVE_DIR = new File("./saves");

	private static final ScheduledThreadPoolExecutor SCHEDULED_EXECUTOR = new ShadbotScheduledExecutor(2, "Shadbot-DataManager-%d");
	private static final List<Runnable> SAVE_TASKS = new ArrayList<>();

	public static boolean init() {
		LogUtils.infof("Initializing data files...");

		if(!SAVE_DIR.exists() && !SAVE_DIR.mkdir()) {
			LogUtils.errorf("The save folder could not be created.");
			return false;
		}

		Reflections reflections = new Reflections(DataManager.class.getPackage().getName(), new MethodAnnotationsScanner());
		for(Method initMethod : reflections.getMethodsAnnotatedWith(DataInit.class)) {

			try {
				initMethod.invoke(null);
			} catch (Exception err) {
				LogUtils.errorf(err, "An error occurred while initializing data %s.", initMethod.getDeclaringClass().getSimpleName());
				return false;
			}

			// Search for save() method and if found, schedule it
			for(Method method : initMethod.getDeclaringClass().getMethods()) {
				if(method.isAnnotationPresent(DataSave.class)) {
					DataSave annotation = method.getAnnotation(DataSave.class);
					Runnable saveTask = () -> {
						try {
							LogUtils.infof("Saving %s...", initMethod.getDeclaringClass().getSimpleName());
							method.invoke(null);
							LogUtils.infof("%s saved.", initMethod.getDeclaringClass().getSimpleName());
						} catch (Exception err) {
							LogUtils.errorf(err, "An error occurred while saving %s.", annotation.filePath());
						}
					};
					SAVE_TASKS.add(saveTask);
					SCHEDULED_EXECUTOR.scheduleAtFixedRate(saveTask, annotation.initialDelay(), annotation.period(), annotation.unit());
				}
			}

			LogUtils.infof("%s initialized.", initMethod.getDeclaringClass().getSimpleName());
		}

		LogUtils.infof("Data files initialized.");
		return true;
	}

	public static void stop() {
		SCHEDULED_EXECUTOR.shutdownNow();
		SAVE_TASKS.stream().forEach(Runnable::run);
	}
}
