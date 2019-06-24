import com.shadorc.shadbot.utils.NumberUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestNumberUtils {

    @Test
    public void test_asInt() {
        assertEquals(Integer.valueOf(14), NumberUtils.asInt("14"));
        assertEquals(Integer.valueOf(14), NumberUtils.asInt("  14   "));
        assertEquals(Integer.valueOf(0), NumberUtils.asInt("0"));
        assertEquals(Integer.valueOf(-14), NumberUtils.asInt("-14"));
        assertEquals(Integer.valueOf(-14), NumberUtils.asInt("  -14   "));
        assertNull(NumberUtils.asInt("quatorze"));
        assertNull(NumberUtils.asInt(null));
        assertNull(NumberUtils.asInt("9223372036854775807"));
    }

    @Test
    public void test_asPositiveInt() {
        assertEquals(Integer.valueOf(14), NumberUtils.asPositiveInt("14"));
        assertEquals(Integer.valueOf(14), NumberUtils.asPositiveInt("  14   "));
        assertNull(NumberUtils.asPositiveInt("0"));
        assertNull(NumberUtils.asPositiveInt("-14"));
        assertNull(NumberUtils.asPositiveInt("  -14   "));
        assertNull(NumberUtils.asPositiveInt("quatorze"));
        assertNull(NumberUtils.asPositiveInt(null));
        assertNull(NumberUtils.asPositiveInt("9223372036854775807"));
    }

    @Test
    public void test_asIntBetween() {
        assertEquals(14, NumberUtils.asIntBetween("14", 10, 20));
        assertNull(NumberUtils.asIntBetween("4", 10, 20));
        assertNull(NumberUtils.asIntBetween("24", 10, 20));
        assertNull(NumberUtils.asIntBetween("-12", 10, 20));
        assertEquals(10, NumberUtils.asIntBetween("10", 10, 20));
        assertEquals(10, NumberUtils.asIntBetween(" 10   ", 10, 20));
        assertNull(NumberUtils.asIntBetween(null, 10, 20));
        assertNull(NumberUtils.asIntBetween("9223372036854775807", 10, 20));
    }

    @Test
    public void test_asLong() {
        assertEquals(Long.valueOf(14), NumberUtils.asLong("14"));
        assertEquals(Long.valueOf(14), NumberUtils.asLong("  14   "));
        assertEquals(Long.valueOf(0), NumberUtils.asLong("0"));
        assertEquals(Long.valueOf(-14), NumberUtils.asLong("-14"));
        assertEquals(Long.valueOf(-14), NumberUtils.asLong("  -14   "));
        assertNull(NumberUtils.asLong("quatorze"));
        assertNull(NumberUtils.asLong(null));
        assertEquals(Long.MAX_VALUE, NumberUtils.asLong("9223372036854775807"));
    }

    @Test
    public void test_asPositiveLong() {
        assertEquals(Long.valueOf(14), NumberUtils.asPositiveLong("14"));
        assertEquals(Long.valueOf(14), NumberUtils.asPositiveLong("  14   "));
        assertNull(NumberUtils.asPositiveLong("0"));
        assertNull(NumberUtils.asPositiveLong("-14"));
        assertNull(NumberUtils.asPositiveLong("  -14   "));
        assertNull(NumberUtils.asPositiveLong("quatorze"));
        assertNull(NumberUtils.asPositiveLong(null));
        assertEquals(Long.MAX_VALUE, NumberUtils.asPositiveLong("9223372036854775807"));
    }

    @Test
    public void test_isPositiveLong() {
        assertTrue(NumberUtils.isPositiveLong("14"));
        assertTrue(NumberUtils.isPositiveLong("  14   "));
        assertFalse(NumberUtils.isPositiveLong("0"));
        assertFalse(NumberUtils.isPositiveLong("-14"));
        assertFalse(NumberUtils.isPositiveLong("  -14   "));
        assertFalse(NumberUtils.isPositiveLong("quatorze"));
        assertFalse(NumberUtils.isPositiveLong(null));
        assertTrue(NumberUtils.isPositiveLong("9223372036854775807"));
    }

    @Test
    public void test_truncateBetween() {
        assertEquals(14, NumberUtils.truncateBetween(14, 10, 20));
        assertEquals(10, NumberUtils.truncateBetween(4, 10, 20));
        assertEquals(20, NumberUtils.truncateBetween(24, 10, 20));
        assertEquals(10, NumberUtils.truncateBetween(-12, 10, 20));
        assertEquals(10, NumberUtils.truncateBetween(10, 10, 20));
    }

    @Test
    public void test_isBetween() {
        assertTrue(NumberUtils.isBetween(14, 10, 20));
        assertFalse(NumberUtils.isBetween(4, 10, 20));
        assertFalse(NumberUtils.isBetween(24, 10, 20));
        assertFalse(NumberUtils.isBetween(-12, 10, 20));
        assertTrue(NumberUtils.isBetween(10, 10, 20));
    }

}
