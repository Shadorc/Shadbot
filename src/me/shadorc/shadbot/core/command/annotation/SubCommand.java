package me.shadorc.shadbot.core.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import me.shadorc.shadbot.core.command.CommandPermission;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SubCommand {

	String[] names();

	CommandPermission permission() default CommandPermission.USER;

}
