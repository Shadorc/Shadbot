package me.shadorc.shadbot.data.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import me.shadorc.shadbot.data.DataManager;

/**
 * All the methods annotated with this interface will be called periodically depending on parameters from the moment {@link DataManager} is called
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataSave {

	String filePath();

	int initialDelay();

	int period();

	TimeUnit unit();

}
