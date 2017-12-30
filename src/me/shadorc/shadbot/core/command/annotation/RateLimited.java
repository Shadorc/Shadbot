package me.shadorc.shadbot.core.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

import me.shadorc.shadbot.utils.ratelimiter.RateLimiter;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {

	int max() default 3;

	int cooldown() default RateLimiter.DEFAULT_COOLDOWN;

	ChronoUnit unit() default ChronoUnit.SECONDS;

}
