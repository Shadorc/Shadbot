package me.shadorc.shadbot.core.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {

	String[] names();

	CommandCategory category();

	CommandPermission permission() default CommandPermission.USER;

	String alias() default "";

	boolean hidden() default false;

}
