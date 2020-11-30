package com.shadorc.shadbot.command;

import com.shadorc.shadbot.utils.LogUtils;
import reactor.core.publisher.Mono;
import reactor.util.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

public abstract class CmdTest<T> {

    private final Logger logger;
    private final Class<T> cmdClass;
    private final T cmd;

    @SuppressWarnings("unchecked")
    public CmdTest() {
        this.cmdClass = (Class<T>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        this.logger = LogUtils.getLogger(cmdClass, LogUtils.Category.TEST);
        try {
            this.cmd = cmdClass.getConstructor().newInstance();
        } catch (final Exception err) {
            throw new RuntimeException(err);
        }
    }

    public <R> R invoke(final String name) {
        return this.invoke(name, new Class<?>[]{}, new Object[]{});
    }

    public <R> R invoke(final String name, final Class<?> clazz, final Object arg) {
        return this.invoke(name, new Class<?>[]{clazz}, new Object[]{arg});
    }

    @SuppressWarnings("unchecked")
    public <R> R invoke(final String name, final Class<?>[] classes, final Object[] args) {
        try {
            final Method method = this.cmdClass.getDeclaredMethod(name, classes);
            method.setAccessible(true);

            final R result = ((Mono<R>) method.invoke(this.cmd, args)).block();
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("{}: {}", this.cmdClass.getSimpleName(), result);
            }
            return result;
        } catch (final Exception err) {
            throw new RuntimeException(err);
        }
    }

}
