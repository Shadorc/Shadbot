package com.shadorc.shadbot.command;

import com.shadorc.shadbot.utils.LogUtil;
import reactor.core.publisher.Mono;
import reactor.util.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;

public abstract class CmdTest<T> {

    private final Logger logger;
    private final Class<T> cmdClass;
    private final T cmd;

    @SuppressWarnings("unchecked")
    public CmdTest() {
        this.cmdClass = (Class<T>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        this.logger = LogUtil.getLogger(this.cmdClass, LogUtil.Category.TEST);
        try {
            this.cmd = this.cmdClass.getConstructor().newInstance();
        } catch (final Exception err) {
            throw new RuntimeException(err);
        }
    }

    @SuppressWarnings("unchecked")
    public <R> R invoke(final String name, final Object... args) {
        try {
            final Class<?>[] classes = Arrays.stream(args).map(Object::getClass).toArray(Class[]::new);
            final Method method = this.cmdClass.getDeclaredMethod(name, classes);
            method.setAccessible(true);

            final Object resultObj = method.invoke(this.cmd, args);
            final R result = resultObj instanceof Mono ? ((Mono<R>) resultObj).block() : (R) resultObj;
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("{}: {}", this.cmdClass.getSimpleName(), result);
            }
            return result;
        } catch (final RuntimeException err) {
            throw err;
        } catch (final Exception err) {
            final Throwable cause = err.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(err);
        }
    }

}
