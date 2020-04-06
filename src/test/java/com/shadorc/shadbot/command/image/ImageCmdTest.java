package com.shadorc.shadbot.command.image;

import com.shadorc.shadbot.api.json.image.deviantart.Image;
import com.shadorc.shadbot.utils.NetUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ImageCmdTest {

    private static Logger logger;
    private static ImageCmd cmd;
    private static Method method;

    @BeforeAll
    public static void init() throws NoSuchMethodException {
        logger = Loggers.getLogger(ImageCmdTest.class);
        cmd = new ImageCmd();

        method = ImageCmd.class.getDeclaredMethod("getPopularImage", String.class);
        method.setAccessible(true);
    }

    @Test
    public void testGetPopularImage() throws InvocationTargetException, IllegalAccessException {
        final Image result = ((Mono<Image>) method.invoke(cmd, "dab")).block();
        logger.debug("testGetPopularImage: {}", result);
        assertNotNull(result.getContent());
        assertNotNull(result.getAuthor());
        assertNotNull(result.getCategoryPath());
        assertNotNull(result.getTitle());
        assertNotNull(result.getUrl());
    }

    @Test
    public void testGetPopularImageSpecial() throws InvocationTargetException, IllegalAccessException {
        final Image result = ((Mono<Image>) method.invoke(cmd,
                NetUtils.encode("&~#{([-|`_\"'\\^@)]=}°+¨^$£¤%*µ,?;.:/!§<>+-*/"))).block();
        logger.debug("testGetPopularImageSpecial: {}", result);
        assertNull(result);
    }

}
