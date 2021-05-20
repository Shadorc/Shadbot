package com.shadorc.shadbot.command;

import com.shadorc.shadbot.core.command.GroupCmd;
import com.shadorc.shadbot.core.command.SubCmd;
import com.shadorc.shadbot.utils.LogUtil;
import reactor.core.publisher.Mono;
import reactor.util.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;

import static org.mockito.Mockito.mock;

public abstract class CmdTest<T> {

    protected static final String SPECIAL_CHARS = "&~#{([-|`_\"'\\^@)]=}°+¨^ $£¤%*µ,?;.:/!§<>+-*/";

    private final Logger logger;
    private final Class<T> cmdClass;
    private final T cmd;

    @SuppressWarnings("unchecked")
    public CmdTest() {
        this.cmdClass = (Class<T>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        this.logger = LogUtil.getLogger(this.cmdClass, LogUtil.Category.TEST);
        try {
            if (SubCmd.class.isAssignableFrom(this.cmdClass)) {
                this.cmd = this.cmdClass.getDeclaredConstructor(GroupCmd.class).newInstance(mock(GroupCmd.class));
            } else {
                this.cmd = this.cmdClass.getConstructor().newInstance();
            }
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
            this.logger.info("{}#{}{} result:\n{}", this.cmdClass.getSimpleName(), name, Arrays.toString(args), result);
            return result;
        } catch (final RuntimeException err) {
            throw err;
        } catch (final Exception err) {
            if (err.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw new RuntimeException(err);
        }
    }

}
