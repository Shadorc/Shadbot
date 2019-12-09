import com.shadorc.shadbot.utils.NumberUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestNumberUtils {

    @Test
    public void testToIntOrNull() {
        assertEquals(Integer.valueOf(14), NumberUtils.toIntOrNull("14"));
        assertEquals(Integer.valueOf(14), NumberUtils.toIntOrNull("  14   "));
        assertEquals(Integer.valueOf(0), NumberUtils.toIntOrNull("0"));
        assertEquals(Integer.valueOf(-14), NumberUtils.toIntOrNull("-14"));
        assertEquals(Integer.valueOf(-14), NumberUtils.toIntOrNull("  -14   "));
        assertNull(NumberUtils.toIntOrNull("fourteen"));
        assertNull(NumberUtils.toIntOrNull(null));
        assertNull(NumberUtils.toIntOrNull("9223372036854775807"));
    }

    @Test
    public void testToPositiveIntOrNull() {
        assertEquals(Integer.valueOf(14), NumberUtils.toPositiveIntOrNull("14"));
        assertEquals(Integer.valueOf(14), NumberUtils.toPositiveIntOrNull("  14   "));
        assertNull(NumberUtils.toPositiveIntOrNull("0"));
        assertNull(NumberUtils.toPositiveIntOrNull("-14"));
        assertNull(NumberUtils.toPositiveIntOrNull("  -14   "));
        assertNull(NumberUtils.toPositiveIntOrNull("fourteen"));
        assertNull(NumberUtils.toPositiveIntOrNull(null));
        assertNull(NumberUtils.toPositiveIntOrNull("9223372036854775807"));
    }

    @Test
    public void testToIntBetweenOrNull() {
        assertEquals(14, NumberUtils.toIntBetweenOrNull("14", 10, 20));
        assertNull(NumberUtils.toIntBetweenOrNull("4", 10, 20));
        assertNull(NumberUtils.toIntBetweenOrNull("24", 10, 20));
        assertNull(NumberUtils.toIntBetweenOrNull("-12", 10, 20));
        assertEquals(10, NumberUtils.toIntBetweenOrNull("10", 10, 20));
        assertEquals(10, NumberUtils.toIntBetweenOrNull(" 10   ", 10, 20));
        assertNull(NumberUtils.toIntBetweenOrNull(null, 10, 20));
        assertNull(NumberUtils.toIntBetweenOrNull("9223372036854775807", 10, 20));
    }

    @Test
    public void testToLongOrNull() {
        assertEquals(Long.valueOf(14), NumberUtils.toLongOrNull("14"));
        assertEquals(Long.valueOf(14), NumberUtils.toLongOrNull("  14   "));
        assertEquals(Long.valueOf(0), NumberUtils.toLongOrNull("0"));
        assertEquals(Long.valueOf(-14), NumberUtils.toLongOrNull("-14"));
        assertEquals(Long.valueOf(-14), NumberUtils.toLongOrNull("  -14   "));
        assertNull(NumberUtils.toLongOrNull("fourteen"));
        assertNull(NumberUtils.toLongOrNull(null));
        assertEquals(Long.MAX_VALUE, NumberUtils.toLongOrNull("9223372036854775807"));
    }

    @Test
    public void testToPositiveLongOrNull() {
        assertEquals(Long.valueOf(14), NumberUtils.toPositiveLongOrNull("14"));
        assertEquals(Long.valueOf(14), NumberUtils.toPositiveLongOrNull("  14   "));
        assertNull(NumberUtils.toPositiveLongOrNull("0"));
        assertNull(NumberUtils.toPositiveLongOrNull("-14"));
        assertNull(NumberUtils.toPositiveLongOrNull("  -14   "));
        assertNull(NumberUtils.toPositiveLongOrNull("fourteen"));
        assertNull(NumberUtils.toPositiveLongOrNull(null));
        assertEquals(Long.MAX_VALUE, NumberUtils.toPositiveLongOrNull("9223372036854775807"));
    }

    @Test
    public void testIsPositiveLong() {
        assertTrue(NumberUtils.isPositiveLong("14"));
        assertTrue(NumberUtils.isPositiveLong("  14   "));
        assertFalse(NumberUtils.isPositiveLong("0"));
        assertFalse(NumberUtils.isPositiveLong("-14"));
        assertFalse(NumberUtils.isPositiveLong("  -14   "));
        assertFalse(NumberUtils.isPositiveLong("fourteen"));
        assertFalse(NumberUtils.isPositiveLong(null));
        assertTrue(NumberUtils.isPositiveLong("9223372036854775807"));
    }

    @Test
    public void testTruncateBetween() {
        assertEquals(14, NumberUtils.truncateBetween(14, 10, 20));
        assertEquals(10, NumberUtils.truncateBetween(4, 10, 20));
        assertEquals(20, NumberUtils.truncateBetween(24, 10, 20));
        assertEquals(10, NumberUtils.truncateBetween(-12, 10, 20));
        assertEquals(10, NumberUtils.truncateBetween(10, 10, 20));
    }

    @Test
    public void testIsBetween() {
        assertTrue(NumberUtils.isBetween(14, 10, 20));
        assertFalse(NumberUtils.isBetween(4, 10, 20));
        assertFalse(NumberUtils.isBetween(24, 10, 20));
        assertFalse(NumberUtils.isBetween(-12, 10, 20));
        assertTrue(NumberUtils.isBetween(10, 10, 20));
    }

}
