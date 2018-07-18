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
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.executor.ScheduledWrappedExecutor;

public class DataManager {

	public static final File SAVE_DIR = new File("./saves");

	private static final ScheduledThreadPoolExecutor SCHEDULED_EXECUTOR = new ScheduledWrappedExecutor(2, "DataManager-%d");
	private static final List<Runnable> SAVE_TASKS = new ArrayList<>();

	public static boolean init() {
		if(!SAVE_DIR.exists() && !SAVE_DIR.mkdir()) {
			LogUtils.error("The save directory could not be created.");
			return false;
		}

		Reflections reflections = new Reflections(DataManager.class.getPackage().getName(), new MethodAnnotationsScanner());
		for(Method initMethod : reflections.getMethodsAnnotatedWith(DataInit.class)) {
			String className = initMethod.getDeclaringClass().getSimpleName();
			try {
				initMethod.invoke(null);
			} catch (Exception err) {
				LogUtils.error(err, String.format("An error occurred while initializing data %s.", className));
				return false;
			}

			// Search for save() method and if found, schedule it
			for(Method method : initMethod.getDeclaringClass().getMethods()) {
				if(method.isAnnotationPresent(DataSave.class)) {
					DataSave annotation = method.getAnnotation(DataSave.class);
					Runnable saveTask = () -> {
						try {
							method.invoke(null);
							LogUtils.infof("%s saved.", className);
						} catch (Exception err) {
							LogUtils.error(err, String.format("An error occurred while saving %s.", className));
						}
					};
					SAVE_TASKS.add(saveTask);
					SCHEDULED_EXECUTOR.scheduleAtFixedRate(saveTask, annotation.initialDelay(), annotation.period(), annotation.unit());
				}
			}
		}

		LogUtils.infof("Data files initialized.");
		return true;
	}

	public static void stop() {
		SCHEDULED_EXECUTOR.shutdownNow();
		SAVE_TASKS.forEach(Runnable::run);
	}
}
