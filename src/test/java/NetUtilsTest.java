import com.shadorc.shadbot.utils.NetUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NetUtilsTest {

    @Test
    public void testCleanWithLinebreaks() {
        assertEquals("Test", NetUtils.cleanWithLinebreaks("<html>Test</html>"));
        assertEquals("", NetUtils.cleanWithLinebreaks("<html></html>"));
        assertEquals("", NetUtils.cleanWithLinebreaks(""));
        assertEquals("", NetUtils.cleanWithLinebreaks(null));
        assertEquals("Hello\nWorld", NetUtils.cleanWithLinebreaks("Hello<br>World"));
        assertEquals("\nHello\nWorld", NetUtils.cleanWithLinebreaks("<p>Hello</p><br>World"));
    }

}
