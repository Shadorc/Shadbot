package me.shadorc.shadbot.data.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import me.shadorc.shadbot.data.DataManager;

/**
 * All the methods annoted with this interface will be loaded when calling {@link DataManager} initialization
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataInit {

}
