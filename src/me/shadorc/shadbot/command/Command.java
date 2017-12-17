package me.shadorc.shadbot.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {

	String[] names();

	String alias() default "";

	boolean hidden() default false;

	CommandPermission role() default CommandPermission.USER;

	Type type() default Type.COMMAND;

}
