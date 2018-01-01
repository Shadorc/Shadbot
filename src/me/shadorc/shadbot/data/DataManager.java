package me.shadorc.shadbot.data;

import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;

import me.shadorc.shadbot.data.annotation.DataInit;
import me.shadorc.shadbot.data.annotation.DataSave;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.ThreadPoolUtils;

public class DataManager {

	private static final ScheduledExecutorService SCHEDULED_EXECUTOR =
			Executors.newScheduledThreadPool(2, ThreadPoolUtils.getThreadFactoryNamed("Shadbot-DataManager-%d"));

	public static boolean init() {
		LogUtils.infof("Initializing data files...");

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
					SCHEDULED_EXECUTOR.scheduleAtFixedRate(saveTask, annotation.initialDelay(), annotation.period(), annotation.unit());
				}
			}

			LogUtils.infof("%s initialized.", initMethod.getDeclaringClass().getSimpleName());
		}

		LogUtils.infof("Data files initialized.");
		return true;
	}

	public static void stop() {
		// Shutdown executor and run waiting tasks
		SCHEDULED_EXECUTOR.shutdownNow().stream().forEach(runnable -> runnable.run());
	}
}
