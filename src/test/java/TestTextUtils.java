import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.shadorc.shadbot.utils.TextUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestTextUtils {

    @Test
    public void testCleanLavaplayerErr() {
        final FriendlyException errWithMsg = new FriendlyException("<url src=\"youtube\">Watch on YouTube</url>Error",
                FriendlyException.Severity.COMMON, null);
        assertEquals("Error", TextUtils.cleanLavaplayerErr(errWithMsg));
        assertThrows(NullPointerException.class, () -> TextUtils.cleanLavaplayerErr(null));

        final FriendlyException errWithoutMsg = new FriendlyException(null, FriendlyException.Severity.COMMON, null);
        assertEquals("Error not specified.", TextUtils.cleanLavaplayerErr(errWithoutMsg));
    }

}
