package me.shadorc.shadbot.data;

import java.io.File;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;

import me.shadorc.shadbot.data.annotation.DataInit;
import me.shadorc.shadbot.data.annotation.DataSave;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class DataManager {

	public static final File SAVE_DIR = new File("./saves");

	private static final List<Runnable> SAVE_TASKS = new ArrayList<>();

	public static boolean init() {
		if(!SAVE_DIR.exists() && !SAVE_DIR.mkdir()) {
			LogUtils.error("The save directory could not be created.");
			return false;
		}

		final Reflections reflections = new Reflections(DataManager.class.getPackage().getName(), new MethodAnnotationsScanner());
		for(Method initMethod : reflections.getMethodsAnnotatedWith(DataInit.class)) {
			final String className = initMethod.getDeclaringClass().getSimpleName();
			try {
				initMethod.invoke(null);
			} catch (Exception err) {
				LogUtils.error(err, String.format("An error occurred while initializing data %s.", className));
				return false;
			}

			// Search for save() method and if found, schedule it
			for(Method method : initMethod.getDeclaringClass().getMethods()) {
				if(method.isAnnotationPresent(DataSave.class)) {
					final DataSave annotation = method.getAnnotation(DataSave.class);
					final Runnable saveTask = () -> {
						try {
							method.invoke(null);
							LogUtils.infof("%s saved.", className);
						} catch (Exception err) {
							LogUtils.error(err, String.format("An error occurred while saving %s.", className));
						}
					};
					SAVE_TASKS.add(saveTask);
					Flux.interval(
							Duration.of(annotation.initialDelay(), annotation.unit()),
							Duration.of(annotation.period(), annotation.unit()))
							.doOnNext(ignored -> Mono.fromRunnable(saveTask))
							.subscribe();
				}
			}
		}

		LogUtils.infof("Data files initialized.");
		return true;
	}

	public static void stop() {
		SAVE_TASKS.forEach(Runnable::run);
	}
}
